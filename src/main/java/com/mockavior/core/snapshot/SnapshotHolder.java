package com.mockavior.runtime.snapshot;

import com.mockavior.core.snapshot.ContractSnapshot;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe holder of the current ContractSnapshot.
 * Uses atomic reference swap for hot reload.
 */
public final class SnapshotHolder {

    private final AtomicReference<ContractSnapshot> current =
            new AtomicReference<>();

    public SnapshotHolder(ContractSnapshot initialSnapshot) {
        this.current.set(
                Objects.requireNonNull(initialSnapshot, "initialSnapshot must not be null")
        );
    }

    /**
     * Returns the snapshot currently used by runtime.
     */
    public ContractSnapshot current() {
        return current.get();
    }

    /**
     * Atomically swaps the current snapshot.
     * All in-flight requests continue using the old snapshot.
     */
    public void swap(ContractSnapshot newSnapshot) {
        current.set(
                Objects.requireNonNull(newSnapshot, "newSnapshot must not be null")
        );
    }
}
