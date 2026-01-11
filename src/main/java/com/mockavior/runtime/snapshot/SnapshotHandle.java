package com.mockavior.runtime.snapshot;

import com.mockavior.behavior.Behavior;
import com.mockavior.core.snapshot.ContractSnapshot;
import lombok.extern.slf4j.Slf4j;


import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public final class SnapshotHandle {

    private final ContractSnapshot snapshot;
    private final AtomicInteger inFlightRequests = new AtomicInteger(0);
    private volatile SnapshotState state = SnapshotState.CREATED;
    private final Behavior fallbackBehavior;


    public SnapshotHandle(ContractSnapshot snapshot, Behavior fallbackBehavior) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot must not be null");
        this.fallbackBehavior = fallbackBehavior;
    }

    public ContractSnapshot snapshot() {
        return snapshot;
    }

    public SnapshotState state() {
        return state;
    }

    void activate() {
        this.state = SnapshotState.ACTIVE;

        log.info(
                "Snapshot activated: version={}",
                snapshot.version().value()
        );
    }

    void retire() {
        this.state = SnapshotState.RETIRED;

        log.info(
                "Snapshot retired: version={}, inFlightRequests={}",
                snapshot.version().value(),
                inFlightRequests.get()
        );
    }

    public void onRequestStart() {
        int current = inFlightRequests.incrementAndGet();

        log.trace(
                "Request started on snapshot: version={}, inFlightRequests={}",
                snapshot.version().value(),
                current
        );
    }

    public void onRequestEnd() {
        int current = inFlightRequests.decrementAndGet();

        log.trace(
                "Request finished on snapshot: version={}, inFlightRequests={}",
                snapshot.version().value(),
                current
        );
    }

    public boolean isIdle() {
        boolean idle = inFlightRequests.get() == 0;

        if (idle && state == SnapshotState.RETIRED) {
            log.trace(
                    "Snapshot is idle and ready for cleanup: version={}",
                    snapshot.version().value()
            );
        }

        return idle;
    }

    public Behavior fallbackBehavior() {
        return fallbackBehavior;
    }
}
