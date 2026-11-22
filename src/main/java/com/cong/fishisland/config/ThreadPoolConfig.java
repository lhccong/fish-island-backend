package com.cong.fishisland.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Description: 线程池配置
 * @author cong
 * @date 2024/02/18
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig implements AsyncConfigurer {
    /**
     * 项目共用线程池
     */
    public static final String WANWU_EXECUTOR = "fishExecutor";
    /**
     * websocket通信线程池
     */
    public static final String WS_EXECUTOR = "websocketExecutor";


    public static final String AI_CHAT_EXECUTOR = "aiChatExecutor";
    public static final String BIG_FISH_EXECUTOR = "bigFishExecutor";
    public static final String BIG_FISH_MEMORY_EXECUTOR = "bigFishMemoryExecutor";

    @Override
    public Executor getAsyncExecutor() {
        return fishExecutor();
    }

    public Executor getSecureInvokeExecutor() {
        return fishExecutor();
    }

    @Bean(WANWU_EXECUTOR)
    @Primary
    public ThreadPoolTaskExecutor fishExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("fish-executor-");
        //满了调用线程执行，认为重要任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadFactory(new MyThreadFactory(executor));
        executor.initialize();
        return executor;
    }

    @Bean(WS_EXECUTOR)
    public ThreadPoolTaskExecutor websocketExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(16);
        //支持同时推送1000人
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("websocket-executor-");
        //满了直接丢弃，默认为不重要消息推送
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setThreadFactory(new MyThreadFactory(executor));
        executor.initialize();
        return executor;
    }

    @Bean(AI_CHAT_EXECUTOR)
    public ThreadPoolTaskExecutor chatAiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(15);
        executor.setThreadNamePrefix("ai-chat-executor-");
        //满了直接丢弃，默认为不重要消息推送
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setThreadFactory(new MyThreadFactory(executor));
        return executor;
    }

    @Bean(BIG_FISH_EXECUTOR)
    public ThreadPoolTaskExecutor bigFishExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("big-fish-");
        return executor;
    }

    @Bean(BIG_FISH_MEMORY_EXECUTOR)
    public ThreadPoolTaskExecutor bigFishMemoryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("big-fish-memory-");
        return executor;
    }
}
