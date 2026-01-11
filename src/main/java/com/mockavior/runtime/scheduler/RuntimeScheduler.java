package com.mockavior.runtime.scheduler;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * RuntimeScheduler is a thin wrapper over ScheduledExecutorService.
 * Responsibilities:
 * - Schedule Task with delay
 * - Return TaskHandle for lifecycle control
 * Scheduler is infrastructure-only and does NOT know business semantics.
 */
@Slf4j
public final class RuntimeScheduler {

    private final ScheduledExecutorService executor;

    public RuntimeScheduler(int threads) {
        this.executor = Executors.newScheduledThreadPool(threads);
        log.info("RuntimeScheduler started with {} threads", threads);
    }

    /**
     * New API (Kafka scenarios).
     * Returns TaskHandle so caller can cancel scheduled work.
     */
    public TaskHandle scheduleTask(Runnable task, Duration delay) {
        Objects.requireNonNull(task, "task must not be null");

        long delayMs = (delay == null) ? 0L : Math.max(0L, delay.toMillis());

        ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Scheduled task failed", e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);

        log.debug("Task scheduled: delayMs={}", delayMs);
        return new TaskHandle(future);
    }

    public void shutdown() {
        log.info("Shutting down RuntimeScheduler");
        executor.shutdown();
    }
}
