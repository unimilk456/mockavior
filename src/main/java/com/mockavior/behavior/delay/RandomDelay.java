package com.mockavior.behavior.delay;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomDelay {

    private final Duration min;
    private final Duration max;

    public RandomDelay(Duration min, Duration max) {
        Objects.requireNonNull(min, "min must not be null");
        Objects.requireNonNull(max, "max must not be null");

        if (min.toMillis() < 0) {
            throw new IllegalArgumentException("min delay must be >= 0");
        }
        if (max.toMillis() < min.toMillis()) {
            throw new IllegalArgumentException("max delay must be >= min delay");
        }

        this.min = min;
        this.max = max;
    }

    public Duration next() {
        long minMs = min.toMillis();
        long maxMs = max.toMillis();

        long value = ThreadLocalRandom.current()
                .nextLong(minMs, maxMs + 1);

        return Duration.ofMillis(value);
    }

    @Override
    public String toString() {
        return "RandomDelay{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
}