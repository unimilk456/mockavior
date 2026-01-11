package com.mockavior.kafka.runtime;

import com.mockavior.kafka.model.KafkaMessage;

import java.util.List;
import java.util.Optional;

/**
 * In-memory storage for Kafka-like messages.
 * This is NOT a Kafka broker.
 * This is a deterministic event store for testing.
 */
public interface InMemoryKafkaStore {

    /**
     * Append message to topic.
     */
    void publish(String topic, KafkaMessage message);

    /**
     * Read all messages without removing them.
     */
    List<KafkaMessage> peek(String topic);

    /**
     * Take (read + remove) first message from topic.
     */
    Optional<KafkaMessage> take(String topic);

    /**
     * Clear all messages for topic.
     */
    void clear(String topic);

    /**
     * @return number of messages published but not yet consumed
     */
    long lag();
}
