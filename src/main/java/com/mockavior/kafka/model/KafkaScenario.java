package com.mockavior.kafka.model;

import java.util.List;
import java.util.Objects;

/**
 * Immutable compiled Kafka scenario.
 * Defines how Kafka messages should be produced during emulation.
 */
public record KafkaScenario(
        String id,
        int repeat,
        List<KafkaRecord> records
) {

    public KafkaScenario {
        Objects.requireNonNull(id, "scenario.id must not be null");
        Objects.requireNonNull(records, "scenario.records must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("scenario.id must not be blank");
        }

        if (repeat <= 0) {
            throw new IllegalArgumentException("scenario.repeat must be >= 1");
        }

        if (records.isEmpty()) {
            throw new IllegalArgumentException("scenario.records must not be empty");
        }

        records = List.copyOf(records);
    }
}
