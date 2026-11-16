package com.cong.fishisland.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ChatMetadataConfig {
    @Bean("chatMetadataExecutor")
    public ThreadPoolTaskExecutor chatMetadataExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("chat-metadata-");
        // Optional metadata must never run on a saturated HTTP caller thread.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
