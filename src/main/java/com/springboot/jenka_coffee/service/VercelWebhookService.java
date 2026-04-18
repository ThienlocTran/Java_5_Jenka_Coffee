package com.springboot.jenka_coffee.service;

public interface VercelWebhookService {
    /**
     * Trigger Vercel rebuild via Deploy Hook
     * Non-blocking, async execution with retry mechanism
     */
    void triggerRebuild();
}
