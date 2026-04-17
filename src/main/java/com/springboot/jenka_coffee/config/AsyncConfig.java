package com.springboot.jenka_coffee.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async Configuration for @Async methods
 * 
 * Thread Pool Strategy:
 * - Core threads: 2 (always alive)
 * - Max threads: 5 (scale up under load)
 * - Queue capacity: 10 (buffer for burst traffic)
 * - Rejection policy: CallerRunsPolicy (backpressure, run in caller thread)
 * 
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
}
