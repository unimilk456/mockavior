package com.mockavior.behavior.delay;

import java.time.Duration;
import java.util.Optional;

public final class DelaySpec {

    private final Duration fixed;
    private final RandomDelay random;

    public DelaySpec(Duration fixed, RandomDelay random) {
        this.fixed = fixed;
        this.random = random;
    }

    public Duration resolve() {
        Duration result = Duration.ZERO;

        if (fixed != null) {
            result = result.plus(fixed);
        }

        if (random != null) {
            result = result.plus(random.next());
        }

        return result;
    }

    @Override
    public String toString() {
        return "DelaySpec{" +
                "fixed=" + fixed +
                ", random=" + random +
                '}';
    }
}