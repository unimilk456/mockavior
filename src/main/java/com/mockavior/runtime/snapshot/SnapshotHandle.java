package com.mockavior.runtime.snapshot;

import com.mockavior.behavior.Behavior;
import com.mockavior.core.snapshot.ContractSnapshot;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;


import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public final class SnapshotHandle {

    private final ContractSnapshot snapshot;
    private final AtomicInteger inFlightRequests = new AtomicInteger(0);
    private volatile SnapshotState state = SnapshotState.CREATED;
    private final Behavior fallbackBehavior;

    // metrics
    private final Timer lifetimeTimer;
    private final Timer retireWaitTimer;
    private final Gauge inFlightGauge;
    private final Gauge activeGauge;

    private final long activatedAtNanos;
    private volatile long retireCalledAtNanos = -1;

    private static final String VERSION = "version";

    public SnapshotHandle(ContractSnapshot snapshot, Behavior fallbackBehavior,  MeterRegistry meterRegistry) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot must not be null");
        this.fallbackBehavior = fallbackBehavior;
        String version = snapshot.version().value();

        this.inFlightGauge = Gauge.builder(
                        "mockavior_snapshot_in_flight",
                        inFlightRequests,
                        AtomicInteger::get
                )
                .tag(VERSION, version)
                .register(meterRegistry);

        this.activeGauge = Gauge.builder(
                        "mockavior_snapshot_active",
                        () -> 1
                )
                .tag(VERSION, version)
                .register(meterRegistry);

        this.lifetimeTimer = Timer.builder("mockavior_snapshot_lifetime_seconds")
                .tag(VERSION, version)
                .register(meterRegistry);

        this.retireWaitTimer = Timer.builder("mockavior_snapshot_retire_wait_seconds")
                .tag(VERSION, version)
                .register(meterRegistry);

        this.activatedAtNanos = System.nanoTime();
    }

    public ContractSnapshot snapshot() {
        return snapshot;
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
        this.retireCalledAtNanos = System.nanoTime();

        long lifetimeNanos = retireCalledAtNanos - activatedAtNanos;
        lifetimeTimer.record(Duration.ofNanos(lifetimeNanos));

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

        if (current == 0 && retireCalledAtNanos > 0) {
            long waitNanos = System.nanoTime() - retireCalledAtNanos;
            retireWaitTimer.record(Duration.ofNanos(waitNanos));
        }

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
