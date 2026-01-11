package com.mockavior.kafka.raw;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
* Raw representation of a Kafka scenario parsed from YAML contract.
* This model allows nullable fields and performs minimal structural validation.
*/
@Getter
public final class RawKafkaScenario {

    private final String id;
    private final Integer repeat;
    private final List<RawKafkaMessage> messages;

    public RawKafkaScenario(
            String id,
            Integer repeat,
            List<RawKafkaMessage> messages
    ) {
        this.id = id;
        this.repeat = repeat;
        this.messages = messages;
    }

    @SuppressWarnings("unchecked")
    public static RawKafkaScenario fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "kafka scenario map must not be null");

        String id = (String) map.get("id");
        Integer repeat = (Integer) map.get("repeat");

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("kafkaScenario.id must not be empty");
        }

        if (repeat != null && repeat <= 0) {
            throw new IllegalArgumentException("kafkaScenario.repeat must be >= 1");
        }

        Object messagesObj = map.get("messages");
        if (!(messagesObj instanceof List<?> list)) {
            throw new IllegalArgumentException("kafkaScenario.messages must be a list");
        }

        if (list.isEmpty()) {
            throw new IllegalArgumentException("kafkaScenario.messages must not be empty");
        }

        List<RawKafkaMessage> messages =
                list.stream()
                        .map(e -> RawKafkaMessage.fromMap((Map<String, Object>) e))
                        .toList();

        return new RawKafkaScenario(id, repeat, messages);
    }
}
