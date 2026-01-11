package com.mockavior.kafka.service;

import com.mockavior.core.snapshot.ContractSnapshot;
import com.mockavior.kafka.model.KafkaScenario;
import com.mockavior.kafka.runtime.InMemoryKafkaStore;
import com.mockavior.kafka.runtime.ScenarioExecutionRegistry;
import com.mockavior.kafka.runtime.ScenarioExecutionRunner;
import com.mockavior.runtime.scheduler.RuntimeScheduler;
import com.mockavior.runtime.snapshot.SnapshotHandle;
import com.mockavior.runtime.snapshot.SnapshotRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service for managing Kafka emulation scenarios.
 *
 * Responsibilities:
 * - Resolve KafkaScenario from active snapshot
 * - Create and start ScenarioExecutionRunner
 * - Stop running executions
 */
@Slf4j
@Service
public class KafkaScenarioService {

    private final SnapshotRegistry snapshotRegistry;
    private final RuntimeScheduler scheduler;
    private final ScenarioExecutionRegistry registry;
    private final InMemoryKafkaStore store;

    public KafkaScenarioService(
            SnapshotRegistry snapshotRegistry,
            RuntimeScheduler scheduler,
            ScenarioExecutionRegistry registry,
            InMemoryKafkaStore store
    ) {
        this.snapshotRegistry = snapshotRegistry;
        this.scheduler = scheduler;
        this.registry = registry;
        this.store = store;
    }

    public ScenarioExecutionRunner startScenario(String scenarioId) {
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");

        SnapshotHandle handle = snapshotRegistry.active();
        ContractSnapshot snapshot = handle.snapshot();

        Map<String, KafkaScenario> scenarios = snapshot.kafkaScenarios();

        if (scenarios.isEmpty()) {
            log.error(
                    "Kafka scenarios not configured: snapshotVersion={}",
                    snapshot.version().value()
            );
            throw new IllegalStateException("Kafka scenarios are not configured");
        }

        KafkaScenario scenario = scenarios.get(scenarioId);
        if (scenario == null) {
            throw new IllegalArgumentException(
                    "Kafka scenario not found: " + scenarioId +
                            ", available=" + scenarios.keySet()
            );
        }

        ScenarioExecutionRunner runner =
                new ScenarioExecutionRunner(
                        scenario,
                        scheduler,
                        store
                );

        registry.register(runner);
        runner.startAsync();

        log.info(
                "Kafka scenario started: scenarioId={}, executionId={}",
                scenarioId,
                runner.executionId()
        );

        return runner;
    }

    public boolean stopExecution(UUID executionId) {
        Objects.requireNonNull(executionId, "executionId must not be null");

        boolean stopped = registry.stopAndRemove(executionId);

        if (stopped) {
            log.info("Kafka execution stopped: executionId={}", executionId);
        } else {
            log.warn("Kafka execution not found: executionId={}", executionId);
        }

        return stopped;
    }
}
