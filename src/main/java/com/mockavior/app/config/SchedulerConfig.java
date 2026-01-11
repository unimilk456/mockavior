package com.mockavior.app.config;

import com.mockavior.runtime.scheduler.RuntimeScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean(destroyMethod = "shutdown")
    public RuntimeScheduler runtimeScheduler() {
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
        return new RuntimeScheduler(threads);
    }
}
