package com.mockavior.app.admin.controller;

import com.mockavior.app.admin.dto.kafka.KafkaMessageDTO;
import com.mockavior.app.admin.dto.kafka.KafkaPollPeekResponse;
import com.mockavior.kafka.model.KafkaMessage;
import com.mockavior.kafka.runtime.InMemoryKafkaStore;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/kafka/poll")
@Tag(
        name = "Admin / Kafka Poll",
        description = "Administrative API for inspecting Kafka emulation topics"
)
public final class KafkaPollController {

    @NonNull
    private final InMemoryKafkaStore store;

    /**
     * Peek messages (non-destructive).
     */
    @GetMapping("/{topic}")
    public ResponseEntity<KafkaPollPeekResponse> peek(@PathVariable String topic) {
        log.debug("ADMIN → Kafka peek requested: topic={}", topic);

        List<KafkaMessage> runtimeMessages = store.peek(topic);
        List<KafkaMessageDTO> messages = runtimeMessages.stream()
                .map(this::toDto)
                .toList();

        log.debug(
                "ADMIN ← Kafka peek result: topic={}, count={}",
                topic,
                messages.size()
        );

        return ResponseEntity.ok(
                new KafkaPollPeekResponse(
                        topic,
                        messages.size(),
                        messages
                )
        );
    }

    /**
     * Take first message (destructive).
     */
    @PostMapping("/{topic}/take")
    public ResponseEntity<KafkaMessageDTO> take(@PathVariable String topic) {
        log.debug("ADMIN → Kafka take requested: topic={}", topic);

        Optional<KafkaMessage> message = store.take(topic);

        if (message.isPresent()) {
            KafkaMessageDTO dto = toDto(message.get());

            log.debug("ADMIN ← Kafka take success: topic={}, key={}", dto.topic(), dto.key());

            return ResponseEntity.ok(dto);
        }

        log.debug("ADMIN ← Kafka take empty: topic={}", topic);

        return ResponseEntity.noContent().build();

    }

    /**
     * Clear topic.
     */
    @PostMapping("/{topic}/clear")
    public ResponseEntity<Map<String, Boolean>> clear(@PathVariable String topic) {
        log.debug("ADMIN → Kafka clear requested: topic={}", topic);

        store.clear(topic);

        log.info("ADMIN ← Kafka topic cleared: topic={}", topic);

        return ResponseEntity.ok().build();
    }

    private KafkaMessageDTO toDto(KafkaMessage message) {
        return new KafkaMessageDTO(
                message.topic(),
                message.key(),
                message.value()
        );
    }
}
