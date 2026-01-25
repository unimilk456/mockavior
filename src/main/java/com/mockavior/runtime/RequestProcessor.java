package com.mockavior.runtime;

import com.mockavior.core.engine.BehaviorEngine;
import com.mockavior.core.engine.EngineResult;
import com.mockavior.core.request.GenericRequest;
import com.mockavior.core.snapshot.ContractSnapshot;
import com.mockavior.runtime.snapshot.SnapshotHandle;
import com.mockavior.runtime.snapshot.SnapshotRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Objects;

/**
 * Runtime request processor that integrates SnapshotHandle lifecycle with core BehaviorEngine.
 * Guarantees:
 * - in-flight requests continue using the same snapshot even during hot reload
 * - new requests use new snapshot after activation
 */
public final class RequestProcessor {

    private final SnapshotRegistry snapshotRegistry;
    private final BehaviorEngine engine;

    private final MeterRegistry meterRegistry;

    private final Timer routeMatchTimer;
    private final Counter routeMatchedCounter;
    private final Counter routeFallbackCounter;

    public RequestProcessor(SnapshotRegistry snapshotRegistry, BehaviorEngine engine, MeterRegistry meterRegistry) {
        this.snapshotRegistry = Objects.requireNonNull(snapshotRegistry, "snapshotRegistry must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");

        this.meterRegistry =
                Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");

        this.routeMatchTimer = Timer.builder("mockavior.routing.match.time")
                .description("Time spent matching routes")
                .publishPercentileHistogram()
                .register(meterRegistry);

        this.routeMatchedCounter = Counter.builder("mockavior.routing.matched.total")
                .description("Requests matched to a route")
                .register(meterRegistry);

        this.routeFallbackCounter = Counter.builder("mockavior.routing.fallback.total")
                .description("Requests routed to fallback")
                .register(meterRegistry);
    }

    public EngineResult process(GenericRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        SnapshotHandle handle = snapshotRegistry.active();
        handle.onRequestStart();

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            ContractSnapshot snapshot = handle.snapshot();

            EngineResult result =
                    engine.handle(snapshot, request, handle.fallbackBehavior());

            if (result.routeId() != null) {
                routeMatchedCounter.increment();
            } else {
                routeFallbackCounter.increment();
            }

            return result;

        } finally {
            sample.stop(routeMatchTimer);
            handle.onRequestEnd();
        }
    }
}
