package com.mockavior.core.engine;

import com.mockavior.behavior.BehaviorResult;
import com.mockavior.core.snapshot.ContractSnapshot;

import java.util.Objects;

/**
 * Engine output: behavior result + snapshot version used for this request.
 * SnapshotVersion is critical for observability and debugging hot reload.
 */
public record EngineResult(
        ContractSnapshot snapshot,
        BehaviorResult behaviorResult,
        String routeId
) {

    public EngineResult {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(behaviorResult, "behaviorResult must not be null");
    }

    @Override
    public String toString() {
        return "EngineResult{" +
                "snapshotVersion=" + snapshot.version().value() +
                ", behaviorResult=" + behaviorResult +
                '}';
    }
}
