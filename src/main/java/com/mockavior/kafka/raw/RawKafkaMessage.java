package com.mockavior.kafka.raw;

import lombok.Getter;

import java.util.Map;
import java.util.Objects;

@Getter
public final class RawKafkaMessage {

    public final String topic;
    public final String key;
    public final Object value;
    public final Integer repeat;
    public final Long delayMs;

    public RawKafkaMessage(
            String topic,
            String key,
            Object value,
            Integer repeat,
            Long delayMs
    ) {
        this.topic = topic;
        this.key = key;
        this.value = value;
        this.repeat = repeat;
        this.delayMs = delayMs;
    }

    public static RawKafkaMessage fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "kafka message map must not be null");

        String topic = (String) map.get("topic");
        String key = (String) map.get("key");
        Object value = map.get("value");
        Integer repeat = (Integer) map.get("repeat");

        Long delayMs = null;
        Object delayObj = map.get("delay");

        if (delayObj instanceof Number n) {
            delayMs = n.longValue();
        }

        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("kafka message.topic must not be empty");
        }

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("kafka message.key must not be empty");
        }

        if (repeat != null && repeat <= 0) {
            throw new IllegalArgumentException("kafka message.repeat must be >= 1");
        }

        if (delayMs != null && delayMs < 0) {
            throw new IllegalArgumentException("kafka message.delay must be >= 0");
        }

        return new RawKafkaMessage(topic, key, value, repeat, delayMs);
    }
}
