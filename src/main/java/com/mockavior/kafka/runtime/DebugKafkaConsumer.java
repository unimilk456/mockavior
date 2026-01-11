package com.mockavior.kafka.runtime;

import com.mockavior.kafka.model.KafkaMessage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "mockavior.kafka.debug-consumer.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public final class DebugKafkaConsumer {

    @NonNull
    private final InMemoryKafkaStore store;

    private final Set<String> topics = Set.of(
            "user.created",
            "user.updated",
            "audit.login",
            "audit.logout"
    );

    @PostConstruct
    void onCreate() {

        log.info("DebugKafkaConsumer CREATED, store={}",System.identityHashCode(store));

    }

    @Scheduled(fixedDelayString = "${mockavior.kafka.debug-consumer.poll-delay-ms:500}")
    public void poll() {

        boolean consumedAny = false;

        for (String topic : topics) {

            Optional<KafkaMessage> message;
            while ((message = store.take(topic)).isPresent()) {
                consumedAny = true;
                KafkaMessage m = message.get();

                log.info("[DEBUG-CONSUMER] topic={}, key={}, value={}",topic,m.key(),m.value());
            }
        }

        if (!consumedAny) {
            log.debug("DebugKafkaConsumer poll: no messages");
        }
    }
}
