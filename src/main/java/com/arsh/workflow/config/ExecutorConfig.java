package com.arsh.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import java.util.concurrent.*;

@Configuration
public class ExecutorConfig {

    @Bean("workflowExecutorPool")
    public ExecutorService workflowExecutorPool() {

        ThreadFactory threadFactory = new CustomizableThreadFactory("wf-exec-");

        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                8,                                  // corePoolSize
                16,                                 // maximumPoolSize
                60, TimeUnit.SECONDS,               // keepAlive
                new ArrayBlockingQueue<>(100),      // queueCapacity
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
        );

        // Wrap with DelegatingSecurityContextExecutorService so SecurityContext is propagated
        return new DelegatingSecurityContextExecutorService(threadPool);
    }
}
