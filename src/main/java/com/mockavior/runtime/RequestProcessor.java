package com.mockavior.runtime;

import com.mockavior.core.engine.BehaviorEngine;
import com.mockavior.core.engine.EngineResult;
import com.mockavior.core.request.GenericRequest;
import com.mockavior.core.snapshot.ContractSnapshot;
import com.mockavior.routing.DefaultRouter;
import com.mockavior.routing.Router;
import com.mockavior.runtime.snapshot.SnapshotHandle;
import com.mockavior.runtime.snapshot.SnapshotRegistry;

import java.util.Objects;

/**
 * Runtime request processor that integrates SnapshotHandle lifecycle with core BehaviorEngine.
 *
 * Guarantees:
 * - in-flight requests continue using the same snapshot even during hot reload
 * - new requests use new snapshot after activation
 */
public final class RequestProcessor {

    private final SnapshotRegistry snapshotRegistry;
    private final BehaviorEngine engine;

    public RequestProcessor(SnapshotRegistry snapshotRegistry, BehaviorEngine engine) {
        this.snapshotRegistry = Objects.requireNonNull(snapshotRegistry, "snapshotRegistry must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    public EngineResult process(GenericRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        SnapshotHandle handle = snapshotRegistry.active();
        handle.onRequestStart();
        try {
            ContractSnapshot snapshot = handle.snapshot();
            return engine.handle(snapshot, request, handle.fallbackBehavior());
        } finally {
            handle.onRequestEnd();
        }
    }
}
