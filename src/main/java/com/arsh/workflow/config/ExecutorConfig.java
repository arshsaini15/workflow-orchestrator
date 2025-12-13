package com.arsh.workflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import java.util.concurrent.*;

@Configuration
public class ExecutorConfig {

    @Value("${workflow.executor.pool.core-size:8}")
    private int corePoolSize;

    @Value("${workflow.executor.pool.max-size:16}")
    private int maxPoolSize;

    @Value("${workflow.executor.pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${workflow.executor.pool.thread-name-prefix:wf-exec-}")
    private String threadPrefix;

    @Bean("workflowExecutorPool")
    public ExecutorService workflowExecutorPool() {

        ThreadFactory threadFactory = new CustomizableThreadFactory(threadPrefix);

        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        return new DelegatingSecurityContextExecutorService(threadPool);
    }
}
