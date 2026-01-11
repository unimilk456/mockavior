package com.mockavior.kafka.runtime;

import com.mockavior.kafka.model.KafkaMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory Kafka-like store.
 * Design notes:
 * - Thread-safe
 * - No ordering guarantees across topics
 * - Does NOT interpret repeat/delay — pure storage
 * - peek() provides a snapshot (eventual consistency)
 */
@Slf4j
@Component
public final class DefaultInMemoryKafkaStore implements InMemoryKafkaStore {

    private final Map<String, Queue<KafkaMessage>> topics =
            new ConcurrentHashMap<>();

    private final AtomicLong published = new AtomicLong();
    private final AtomicLong consumed = new AtomicLong();

    @Override
    public void publish(String topic, KafkaMessage message) {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(message, "message must not be null");

        Queue<KafkaMessage> queue = topics.computeIfAbsent(
                topic,
                t -> new ConcurrentLinkedQueue<>()
        );

        queue.add(message);

        published.incrementAndGet();

        log.debug("KafkaStore publish: topic={}, key={}", topic, message.key());
    }

    /**
     * Non-destructive read.
     * Returns a snapshot of current messages in topic.
     */
    @Override
    public List<KafkaMessage> peek(String topic) {
        Queue<KafkaMessage> queue = topics.get(topic);
        if (queue == null) {
            return List.of();
        }

        List<KafkaMessage> snapshot = List.copyOf(queue);

        log.debug("KafkaStore PEEK: topic={}, messages={}", topic, snapshot.size());

        return snapshot;
    }

    /**
     * Destructive read.
     */
    @Override
    public Optional<KafkaMessage> take(String topic) {
        Queue<KafkaMessage> queue = topics.get(topic);
        if (queue == null) {
            return Optional.empty();
        }

        KafkaMessage message = queue.poll();

        if (message != null) {
            consumed.incrementAndGet();
            log.debug("KafkaStore TAKE: topic={}, key={}, remaining={}",topic,message.key(),queue.size());
        }

        return Optional.ofNullable(message);
    }

    @Override
    public void clear(String topic) {
        Queue<KafkaMessage> queue = topics.remove(topic);
        int cleared = queue == null ? 0 : queue.size();

        if (cleared > 0) {
            consumed.addAndGet(cleared);
        }

        log.info("KafkaStore clear: topic={}, messages={}", topic, cleared);
    }

    public long lag() {

        return published.get() - consumed.get();
    }
}
