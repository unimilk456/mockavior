package com.mockavior.kafka.runtime;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ScenarioExecutionRegistry stores and manages running scenario executions.
 * Responsibilities:
 * - Register new execution runners
 * - Find runner by executionId
 * - Stop and remove executions
 * Notes:
 * - Execution state lives inside ScenarioExecutionRunner/ScenarioExecution.
 * - Registry is about access & lifecycle management from outside (HTTP endpoints).
 */
@Slf4j
public final class ScenarioExecutionRegistry {

    private final ConcurrentMap<UUID, ScenarioExecutionRunner> executions = new ConcurrentHashMap<>();

    /**
     * Registers a new runner. If same ID exists (extremely unlikely), fails fast.
     */
    public void register(ScenarioExecutionRunner runner) {
        Objects.requireNonNull(runner, "runner must not be null");

        UUID id = runner.executionId();
        ScenarioExecutionRunner prev = executions.putIfAbsent(id, runner);
        if (prev != null) {
            throw new IllegalStateException("Execution already registered: " + id);
        }

        log.info("Execution registered: executionId={}, scenarioId={}", id, runner.scenarioId());
    }

    /**
     * Stops execution if found and removes it from registry.
     */
    public boolean stopAndRemove(UUID executionId) {
        Objects.requireNonNull(executionId, "executionId must not be null");

        ScenarioExecutionRunner runner = executions.remove(executionId);
        if (runner == null) {
            log.warn("Stop requested but execution not found: executionId={}", executionId);
            return false;
        }

        try {
            runner.stop();
        } catch (Exception e) {
            log.error(
                    "Failed to stop execution: executionId={}",
                    executionId,
                    e
            );
        }

        log.info("Execution removed: executionId={}", executionId);
        return true;
    }

}
