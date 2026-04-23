package com.springboot.jenka_coffee.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async Configuration for @Async methods + RestTemplate
 * Thread Pool Strategy:
 * - Core threads: 2 (always alive)
 * - Max threads: 5 (scale up under load)
 * - Queue capacity: 10 (buffer for burst traffic)
 * - Rejection policy: CallerRunsPolicy (backpressure, run in caller thread)
 * RestTemplate Configuration:
 * - Connection timeout: 10s
 * - Read timeout: 10s
 * Use case: VercelWebhookService async triggers
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Thread pool sizing
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        
        // Thread naming for debugging
        executor.setThreadNamePrefix("async-webhook-");
        
        // Rejection policy: Run in caller thread if pool + queue full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("Async TaskExecutor initialized: core={}, max={}, queue={}", 
                executor.getCorePoolSize(), 
                executor.getMaxPoolSize(), 
                executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * RestTemplate bean for HTTP requests
     * Configuration:
     * - Connection timeout: 10 seconds
     * - Read timeout: 10 seconds
     * Used by: VercelWebhookService, GoogleOAuthService, etc.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        
        log.info("RestTemplate bean initialized with 10s timeout");
        
        return restTemplate;
    }
}
