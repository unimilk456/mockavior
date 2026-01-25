package com.mockavior.runtime.snapshot;

import com.mockavior.behavior.Behavior;
import com.mockavior.core.snapshot.ContractSnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class SnapshotRegistry {

    private final AtomicReference<SnapshotHandle> active =
            new AtomicReference<>();

    private final Set<SnapshotHandle> retired =
            ConcurrentHashMap.newKeySet();

    private final MeterRegistry meterRegistry;

    public SnapshotRegistry(
            ContractSnapshot initialSnapshot,
            Behavior fallbackBehavior,
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = meterRegistry;
        SnapshotHandle handle =
                new SnapshotHandle(initialSnapshot, fallbackBehavior, meterRegistry);
        handle.activate();
        active.set(handle);

        log.info(
                "Initial snapshot activated: version={}",
                initialSnapshot.version().value()
        );
    }

    public SnapshotHandle active() {
        SnapshotHandle handle = active.get();
        log.trace(
                "Returning active snapshot handle: version={}",
                handle.snapshot().version().value()
        );
        return handle;
    }

    public void activateNew(ContractSnapshot newSnapshot, Behavior fallbackBehavior) {
        SnapshotHandle newHandle = new SnapshotHandle(newSnapshot, fallbackBehavior, meterRegistry);
        newHandle.activate();

        SnapshotHandle old = active.getAndSet(newHandle);
        old.retire();
        retired.add(old);

        log.info(
                "Snapshot swapped: newVersion={}, oldVersion={}",
                newSnapshot.version().value(),
                old.snapshot().version().value()
        );
    }

    public void cleanupRetired() {
        int before = retired.size();

        retired.removeIf(handle -> {
            boolean idle = handle.isIdle();
            if (idle) {
                log.trace(
                        "Removing retired idle snapshot: version={}",
                        handle.snapshot().version().value()
                );
            }
            return idle;
        });

        int after = retired.size();

        if (before != after) {
            log.debug(
                    "Cleaned up retired snapshots: before={}, after={}",
                    before,
                    after
            );
        }
    }
}
