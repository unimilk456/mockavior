package com.mockavior.kafka.model;

import com.mockavior.behavior.delay.DelaySpec;

import java.util.Objects;

/**
 * Immutable runtime representation of a Kafka message.
 * Used inside compiled Kafka scenarios.
 */
public record KafkaMessage(
        String topic,
        String key,
        Object value,
        int repeat,
        DelaySpec delay
) {

    public KafkaMessage {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(delay, "delay must not be null");

        if (topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }

        if (repeat <= 0) {
            throw new IllegalArgumentException("message.repeat must be >= 1");
        }
    }
}
