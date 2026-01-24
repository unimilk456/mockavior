package com.mockavior.kafka.raw;

import com.mockavior.behavior.delay.DelaySpec;
import com.mockavior.behavior.delay.RandomDelay;
import lombok.Getter;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@Getter
public final class RawKafkaMessage {

    public final String topic;
    public final String key;
    public final Object value;
    public final Integer repeat;
    public final DelaySpec delay;

    public RawKafkaMessage(
            String topic,
            String key,
            Object value,
            Integer repeat,
            DelaySpec delay
    ) {
        this.topic = topic;
        this.key = key;
        this.value = value;
        this.repeat = repeat;
        this.delay = delay;
    }

    public static RawKafkaMessage fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "kafka message map must not be null");

        String topic = (String) map.get("topic");
        String key = (String) map.get("key");
        Object value = map.get("value");
        Integer repeat = (Integer) map.get("repeat");


        DelaySpec delay = parseDelay(map.get("delay"));

        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("kafka message.topic must not be empty");
        }

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("kafka message.key must not be empty");
        }

        if (repeat != null && repeat <= 0) {
            throw new IllegalArgumentException("kafka message.repeat must be >= 1");
        }


        return new RawKafkaMessage(topic, key, value, repeat, delay);
    }
    private static DelaySpec parseDelay(Object value) {
        if (value == null) {
            return new DelaySpec(Duration.ZERO, null);
        }

        if (value instanceof Number n) {
            return new DelaySpec(Duration.ofMillis(n.longValue()), null);
        }

        if (value instanceof Map<?, ?> map) {
            Duration fixed = null;
            RandomDelay random = null;

            Object fixedRaw = map.get("fixed");
            if (fixedRaw instanceof Number n) {
                fixed = Duration.ofMillis(n.longValue());
            }

            Object randomRaw = map.get("random");
            if (randomRaw instanceof Map<?, ?> rnd) {
                Object minRaw = rnd.get("min");
                Object maxRaw = rnd.get("max");

                if (!(minRaw instanceof Number) || !(maxRaw instanceof Number)) {
                    throw new IllegalArgumentException("kafka message.delay.random.min/max must be numbers");
                }

                random = new RandomDelay(
                        Duration.ofMillis(((Number) minRaw).longValue()),
                        Duration.ofMillis(((Number) maxRaw).longValue())
                );
            }

            return new DelaySpec(
                    fixed != null ? fixed : Duration.ZERO,
                    random
            );
        }

        throw new IllegalArgumentException("Unsupported kafka message.delay value: " + value);
    }
}