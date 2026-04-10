package com.springboot.jenka_coffee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Giới hạn thread pool cho @Async (email, notifications)
 * Tránh tạo thread không giới hạn khi có nhiều đơn hàng đồng thời
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);       // 2 thread thường trực
        executor.setMaxPoolSize(5);        // tối đa 5 thread đồng thời
        executor.setQueueCapacity(50);     // queue 50 task trước khi reject
        executor.setThreadNamePrefix("async-email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
