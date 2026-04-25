package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.service.VercelWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ============================================================================
 * VERCEL WEBHOOK SERVICE - Self-Healing Pipeline
 * ============================================================================

 * Purpose: Trigger Vercel rebuild when product/news data changes

 * Thread Safety Strategy:
 * - AtomicLong lastTriggerTime: Track last successful trigger (cooldown 60s)
 * - AtomicBoolean isTriggering: Lock-free flag to prevent concurrent triggers
 * - NO synchronized blocks: Avoid thread blocking and potential deadlocks

 * Retry Strategy:
 * - Manual retry loop (NOT @Retryable): Full control over retry scope
 * - Exponential backoff: 2s → 4s → 8s (3 attempts max)
 * - Try-finally pattern: Guaranteed cleanup even on crash/timeout

 * Trade-offs:
 * - Consistency over Freshness: Skip concurrent requests during trigger
 * - Eventual consistency: Vercel rebuild takes ~2-3 minutes
 * - Idempotent: Multiple triggers = same result (safe to retry)

 * Error Handling:
 * - Network timeout: Retry with backoff
 * - 4xx errors: Log and skip (bad webhook URL)
 * - 5xx errors: Retry (Vercel temporary issue)
 * - Finally block: Always release isTriggering flag
 * ============================================================================
 */
@Slf4j
@Service
public class VercelWebhookServiceImpl implements VercelWebhookService {

    @Value("${vercel.deploy-hook-url:}")
    private String deployHookUrl;

    private final RestTemplate restTemplate;

    // Thread-safe state management (lock-free)
    private final AtomicLong lastTriggerTime = new AtomicLong(0);
    private final AtomicBoolean isTriggering = new AtomicBoolean(false);

    // Configuration constants
    private static final long COOLDOWN_MS = 60_000; // 60 seconds
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2_000; // 2 seconds


    public VercelWebhookServiceImpl(RestTemplate restTemplate) {
        // VULN-EXTERNAL-CALL-DOS FIX: Configure timeout for external HTTP calls
        // Without timeout, thread can block indefinitely if Vercel is slow/hanging
        // This prevents thread pool exhaustion and service degradation
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds to establish connection
        factory.setReadTimeout(10000);   // 10 seconds to read response
        
        this.restTemplate = new RestTemplate(factory);
        log.info("[VERCEL] RestTemplate configured with timeouts: connect=5s, read=10s");
    }

    /**
     * Trigger Vercel rebuild asynchronously

     * Flow:
     * 1. Check if webhook URL is configured
     * 2. Check cooldown period (60s since last trigger)
     * 3. Acquire trigger lock (atomic CAS operation)
     * 4. Execute HTTP POST with retry + exponential backoff
     * 5. Release lock in finally block (guaranteed cleanup)
     */
    @Override
    @Async("taskExecutor") // Run in separate thread pool
    public void triggerRebuild() {
        // Guard: Check if webhook URL is configured
        if (deployHookUrl == null || deployHookUrl.isBlank()) {
            log.warn("[VERCEL] Deploy hook URL not configured. Skipping rebuild trigger.");
            return;
        }

        // Capture timestamp ONCE for consistent cooldown check and update
        long now = System.currentTimeMillis();
        long lastTrigger = lastTriggerTime.get();
        
        // Guard: Check cooldown period (prevent spam)
        if (now - lastTrigger < COOLDOWN_MS) {
            long remainingSeconds = (COOLDOWN_MS - (now - lastTrigger)) / 1000;
            log.info("[VERCEL] Trigger skipped (Cooldown: {}s remaining). Prioritizing consistency over freshness.", 
                    remainingSeconds);
            return;
        }

        // Guard: Check if another trigger is in progress (lock-free check)
        // compareAndSet: atomic operation, returns true if successfully changed false→true
        if (!isTriggering.compareAndSet(false, true)) {
            log.info("[VERCEL] Trigger skipped (Another trigger in progress). Prioritizing consistency over freshness.");
            return;
        }

        // Update lastTriggerTime immediately after acquiring lock
        // This prevents spam when Vercel is down (cooldown applies regardless of success/failure)
        lastTriggerTime.set(now);

        // ========================================================================
        // CRITICAL SECTION: Trigger with retry + exponential backoff
        // ========================================================================
        try {
            boolean success = false;
            int attempt = 0;

            // Manual retry loop (NOT @Retryable for full control)
            while (attempt < MAX_RETRIES && !success) {
                attempt++;
                
                try {
                    // Execute HTTP POST to Vercel Deploy Hook
                    log.info("[VERCEL] Triggering rebuild (Attempt {}/{})...", attempt, MAX_RETRIES);
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<String> request = new HttpEntity<>("{}", headers);
                    
                    ResponseEntity<String> response = restTemplate.postForEntity(
                            deployHookUrl, 
                            request, 
                            String.class
                    );

                    // Check response status
                    if (response.getStatusCode().is2xxSuccessful()) {
                        success = true;
                        log.info("[VERCEL] ✓ Trigger build SUCCESS (Attempt {}/{}). Vercel will rebuild in ~2-3 minutes.", 
                                attempt, MAX_RETRIES);
                    } else {
                        log.warn("[VERCEL] ✗ Trigger build FAILED (Attempt {}/{}). HTTP Status: {}", 
                                attempt, MAX_RETRIES, response.getStatusCode());
                        
                        // Don't retry on 4xx errors (client error, bad webhook URL)
                        if (response.getStatusCode().is4xxClientError()) {
                            log.error("[VERCEL] 4xx Client Error. Check VERCEL_DEPLOY_HOOK_URL configuration. Aborting retries.");
                            break;
                        }
                    }

                } catch (Exception e) {
                    log.warn("[VERCEL] ✗ Trigger build FAILED (Attempt {}/{}). Error: {}", 
                            attempt, MAX_RETRIES, e.getMessage());
                    
                    // Log full stack trace only on last attempt
                    if (attempt == MAX_RETRIES) {
                        log.error("[VERCEL] All retry attempts exhausted. Last error:", e);
                    }
                }

                // Exponential backoff: 2s → 4s → 8s
                if (!success && attempt < MAX_RETRIES) {
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1)); // 2^(n-1) * 2000
                    log.info("[VERCEL] Retrying in {}ms (Exponential backoff)...", backoffMs);
                    
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // Restore interrupt status
                        log.warn("[VERCEL] Retry interrupted. Aborting.");
                        break;
                    }
                }
            }

            // Final status log
            if (!success) {
                log.error("[VERCEL] ✗ Trigger build FAILED after {} attempts. Manual intervention may be required.", 
                        MAX_RETRIES);
            }

        } finally {
            // ====================================================================
            // CRITICAL: Always release lock, even on crash/timeout/exception
            // ====================================================================
            isTriggering.set(false);
            log.debug("[VERCEL] Trigger lock released.");
        }
    }
}
