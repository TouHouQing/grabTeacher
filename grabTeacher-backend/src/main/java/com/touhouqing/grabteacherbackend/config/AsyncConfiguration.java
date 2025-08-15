package com.touhouqing.grabteacherbackend.config;

import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步执行器配置：
 * - 使用 spring.task.execution.* 配置
 * - 统一为 @Async 提供线程池，避免使用 SimpleAsyncTaskExecutor
 */
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    private final TaskExecutionProperties taskProps;

    public AsyncConfiguration(TaskExecutionProperties taskProps) {
        this.taskProps = taskProps;
    }

    @Bean(name = "applicationTaskExecutor")
    @Primary
    public ThreadPoolTaskExecutor applicationTaskExecutor() {
        TaskExecutionProperties.Pool pool = taskProps.getPool();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(pool.getCoreSize());
        executor.setMaxPoolSize(pool.getMaxSize());
        executor.setQueueCapacity(pool.getQueueCapacity());
        if (pool.getKeepAlive() != null) {
            executor.setKeepAliveSeconds((int) pool.getKeepAlive().toSeconds());
        }
        executor.setThreadNamePrefix(taskProps.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        // 让 @Async 默认使用上面的 applicationTaskExecutor
        return applicationTaskExecutor();
    }
}

