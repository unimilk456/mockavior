package com.mockavior.kafka.runtime;

import com.mockavior.kafka.model.KafkaMessage;
import com.mockavior.kafka.model.KafkaRecord;
import com.mockavior.kafka.model.KafkaScenario;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single deterministic execution of a KafkaScenario.
 * Responsibilities:
 * - Track execution lifecycle (CREATED → RUNNING → FINISHED / STOPPED)
 * - Iterate scenario.repeat × messages × message.repeat
 * - Produce next KafkaRecord on demand
 * Design notes:
 * - NOT thread-safe
 * - Does NOT handle delays, scheduling or Kafka APIs
 * - Fully deterministic and unit-testable
 */
@Slf4j
public final class ScenarioExecution {

    private final UUID executionId;
    private final KafkaScenario scenario;

    private ExecutionState state = ExecutionState.CREATED;

    // Iteration pointers
    private int scenarioRepeatIndex = 0;
    private int messageIndex = 0;
    private int messageRepeatIndex = 0;

    public ScenarioExecution(KafkaScenario scenario) {
        this.executionId = UUID.randomUUID();
        this.scenario = Objects.requireNonNull(scenario, "scenario must not be null");

        log.info(
                "ScenarioExecution created: executionId={}, scenarioId={}, scenarioRepeat={}",
                executionId,
                scenario.id(),
                scenario.repeat()
        );
    }

    /**
     * Starts execution.
     * Transitions state from CREATED → RUNNING.
     */
    public void start() {
        ensureState(ExecutionState.CREATED);

        state = ExecutionState.RUNNING;

        log.info(
                "ScenarioExecution started: executionId={}, scenarioId={}",
                executionId,
                scenario.id()
        );
    }

    /**
     * Requests execution stop.
     * Execution will not produce any further messages.
     */
    public void stop() {
        if (state == ExecutionState.FINISHED || state == ExecutionState.STOPPED) {
            log.warn("Stop requested for already finished execution: executionId={}, state={}", executionId, state);
            return;
        }

        state = ExecutionState.STOPPED;

        log.info("ScenarioExecution stopped: executionId={}, scenarioId={}", executionId, scenario.id());
    }

    /**
     * Returns the next KafkaMessage to emit, or null if execution is finished.
     * This method:
     * - advances internal pointers
     * - respects scenario.repeat and message.repeat
     * - returns messages in deterministic order
     */
    public KafkaRecord nextRecord() {
        if (state != ExecutionState.RUNNING) {

            log.debug("nextMessage() called but execution not running: executionId={}, state={}", executionId, state);

            return null;
        }

        if (scenarioRepeatIndex >= scenario.repeat()) {
            log.debug("Scenario repeats exhausted: executionId={}", executionId);
            finish();
            return null;
        }

        List<KafkaRecord> records = scenario.records();
        if (records.isEmpty()) {
            state = ExecutionState.FINISHED;
            return null;
        }

        KafkaRecord kafkaRecord = scenario.records().get(messageIndex);

        log.debug(
                "Next message selected: executionId={}, scenarioRepeat={}, messageIndex={}, messageRepeat={}, key={}",
                executionId,
                scenarioRepeatIndex,
                messageIndex,
                messageRepeatIndex,
                kafkaRecord.message().key()
        );

        advancePointers(kafkaRecord.message());

        return kafkaRecord;
    }

    /**
     * Advances iteration pointers after emitting a message.
     */
    private void advancePointers(KafkaMessage message) {
        messageRepeatIndex++;

        if (messageRepeatIndex < message.repeat()) {
            // same message, next repeat
            return;
        }

        // move to next message
        messageRepeatIndex = 0;
        messageIndex++;

        if (messageIndex < scenario.records().size()) {
            return;
        }

        // end of message list → next scenario repeat
        messageIndex = 0;
        scenarioRepeatIndex++;
    }

    /**
     * Marks execution as FINISHED.
     */
    private void finish() {
        state = ExecutionState.FINISHED;

        log.info("ScenarioExecution finished naturally: executionId={}, scenarioId={}", executionId, scenario.id());
    }

    private void ensureState(ExecutionState expected) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Invalid execution state: expected=" + expected + ", actual=" + state
            );
        }
    }

    public UUID executionId() {
        return executionId;
    }

    public ExecutionState state() {
        return state;
    }

    public String scenarioId() {
        return scenario.id();
    }

    @Override
    public String toString() {
        return "ScenarioExecution{" +
                "executionId=" + executionId +
                ", scenarioId=" + scenario.id() +
                ", state=" + state +
                '}';
    }
}
