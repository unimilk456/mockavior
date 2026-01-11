package com.mockavior.kafka.runtime;

import com.mockavior.kafka.model.KafkaRecord;
import com.mockavior.kafka.model.KafkaScenario;
import com.mockavior.runtime.scheduler.RuntimeScheduler;
import com.mockavior.runtime.scheduler.TaskHandle;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * ScenarioExecutionRunner drives a ScenarioExecution asynchronously
 * and publishes messages into InMemoryKafkaStore.
 * Responsibilities:
 * - Start execution
 * - Schedule message emission respecting delays
 * - Publish messages into store
 * - Track scheduled tasks for cancellation
 * Runner does NOT know who consumes messages.
 */
@Slf4j
public final class ScenarioExecutionRunner {

    private final ScenarioExecution execution;
    private final RuntimeScheduler scheduler;
    private final InMemoryKafkaStore store;

    private final List<TaskHandle> scheduledTasks = new ArrayList<>();

    public ScenarioExecutionRunner(
            KafkaScenario scenario,
            RuntimeScheduler scheduler,
            InMemoryKafkaStore store
    ) {
        Objects.requireNonNull(scenario, "scenario must not be null");
        this.execution = new ScenarioExecution(scenario);
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    /**
     * Starts scenario execution asynchronously.
     */
    public void startAsync() {
        try {
            execution.start();
        } catch (IllegalStateException e) {
            log.error("Failed to start execution: executionId={}, state={}", executionId(), execution.state(), e);
            throw e;
        }

        log.info("Runner started: executionId={}, scenarioId={}", executionId(), scenarioId());

        scheduleNext();
    }

    /**
     * Stops execution and cancels all scheduled tasks.
     */
    public void stop() {
        execution.stop();

        log.info("Runner stopping: executionId={}, scheduledTasks={}", executionId(), scheduledTasks.size());

        scheduledTasks.forEach(TaskHandle::cancel);
        scheduledTasks.clear();

        log.info(
                "Runner stopped: executionId={}, state={}",
                executionId(),
                execution.state()
        );
    }

    /**
     * Schedules the next message emission step.
     * Each step schedules exactly one task.
     */
    private void scheduleNext() {
        KafkaRecord nextRecord = execution.nextRecord();

        if (nextRecord == null) {
            onExecutionFinished();
            return;
        }

        Duration delay =
                nextRecord.message().delay() == null
                        ? Duration.ZERO
                        : nextRecord.message().delay();

        TaskHandle handle = scheduler.scheduleTask(() -> {
            if (execution.state() != ExecutionState.RUNNING) {
                log.debug(
                        "Skip emission (execution not running): executionId={}, state={}",
                        executionId(),
                        execution.state()
                );
                return;
            }

            store.publish(nextRecord.message().topic(), nextRecord.message());

            log.info("KafkaEmu published: executionId={}, topic={}, key={}", executionId(), nextRecord.message().topic(), nextRecord.message().key());

            // Chain next step
            scheduleNext();

        }, delay);

        scheduledTasks.add(handle);

        log.debug(
                "Next message scheduled: executionId={}, topic={}, delayMs={}",
                executionId(),
                nextRecord.message().topic(),
                delay.toMillis()
        );
    }

    public UUID executionId() {
        return execution.executionId();
    }

    public String scenarioId() {
        return execution.scenarioId();
    }

    public ExecutionState state() {
        return execution.state();
    }

    private void onExecutionFinished() {
        long lag = store.lag();

        if (lag == 0) {
            log.info(
                    "Runner finished cleanly: executionId={}, scenarioId={}",
                    executionId(),
                    scenarioId()
            );
        } else {
            log.warn(
                    "Runner finished with Kafka lag: executionId={}, scenarioId={}, lag={}",
                    executionId(),
                    scenarioId(),
                    lag
            );
        }
    }
}
