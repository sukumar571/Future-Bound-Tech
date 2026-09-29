package com.futureboundtech.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables the background work the notification feature depends on:
 * scheduled class/assignment reminders and non-blocking e-mail delivery.
 *
 * <p>The e-mail pool is deliberately tiny and bounded — notifications are a
 * best-effort side channel, so they must never be able to starve request threads.</p>
 */
@Configuration
@EnableAsync
@EnableScheduling
public class NotificationInfraConfig {

    @Bean("emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("fbt-email-");
        executor.initialize();
        return executor;
    }
}
