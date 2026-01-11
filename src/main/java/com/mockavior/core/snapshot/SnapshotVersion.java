package com.mockavior.core.snapshot;

import java.util.Objects;
import java.util.UUID;

/**
 * Logical identifier of a runtime contract snapshot.
 * Used for hot reload, observability and debugging.
 */
public final class SnapshotVersion {

    private final String value;

    private SnapshotVersion(String value) {

        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public static SnapshotVersion next() {
        return new SnapshotVersion(UUID.randomUUID().toString());
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SnapshotVersion that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
