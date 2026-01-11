package com.mockavior.runtime.scheduler;

import java.util.concurrent.ScheduledFuture;

/**
 * TaskHandle is a control handle for a scheduled task.
 * Responsibilities:
 * - Allow cancellation of the scheduled execution
 * - Provide basic state inspection (done/cancelled)
 * Notes:
 * - Handle controls scheduler-level execution (future), not business state.
 */
public final class TaskHandle {

    private final ScheduledFuture<?> future;

    TaskHandle(ScheduledFuture<?> future) {
        this.future = future;
    }

    /**
     * Cancels task execution.
     * If the task has not started yet, it will not run.
     */
    public void cancel() {
        future.cancel(false);
    }
}
