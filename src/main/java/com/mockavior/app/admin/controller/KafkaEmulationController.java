package com.mockavior.app.admin.controller;

import com.mockavior.kafka.runtime.ScenarioExecutionRunner;
import com.mockavior.kafka.service.KafkaScenarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoints for Kafka emulation mode.
 * Start:
 *  POST /__mockavior__/kafka/start/{scenarioId}
 * Stop:
 *  POST /__mockavior__/kafka/stop/{executionId}
 */

@Slf4j
@RestController
@RequestMapping("/kafka")
@RequiredArgsConstructor
@Tag(
        name = "Admin / Kafka Emulation",
        description = "Administrative API for starting and stopping Kafka emulation scenarios"
)
public final class KafkaEmulationController {

    @NonNull
    private final KafkaScenarioService service;

    private static final String ERROR = "error";

    /**
     * Starts Kafka emulation scenario from active snapshot.
     */
    @PostMapping("/start/{scenarioId}")
    public ResponseEntity<Map<String, String>> start(@PathVariable String scenarioId) {
        try {
            ScenarioExecutionRunner runner = service.startScenario(scenarioId);

            log.info("ADMIN ← Kafka scenario started: executionId={}, scenarioId={}, state={}", runner.executionId(), runner.scenarioId(), runner.state());

            return ResponseEntity.ok(
                    Map.of(
                            "executionId", runner.executionId().toString(),
                            "scenarioId", runner.scenarioId(),
                            "state", runner.state().name()
                    )
            );
        } catch (IllegalArgumentException e) {

            log.warn("ADMIN ← Kafka scenario not found: scenarioId={}", scenarioId);

            return ResponseEntity.status(404).body(Map.of(ERROR, e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("ADMIN ← Kafka scenario start conflict: scenarioId={}, reason={}", scenarioId, e.getMessage());

            return ResponseEntity.status(409).body(Map.of(ERROR, e.getMessage()));
        }
    }


    /**
     * Stops a previously started execution.
     */
    @PostMapping("/stop/{executionId}")
    public ResponseEntity<Map<String, Object>> stop(@PathVariable String executionId) {
        UUID id;
        try {
            id = UUID.fromString(executionId);
        } catch (IllegalArgumentException e) {
            log.warn("ADMIN ← Invalid executionId UUID: {}", executionId);
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR, "Invalid executionId UUID"));
        }

        boolean stopped = service.stopExecution(id);
        if (!stopped) {
            log.warn("ADMIN ← Kafka execution not found: executionId={}", id);
            return ResponseEntity.status(404).body(Map.of(ERROR, "Execution not found"));
        }

        log.info("ADMIN ← Kafka execution stopped: executionId={}", id);

        return ResponseEntity.ok(Map.of("executionId", id.toString(), "stopped", true));
    }
}
