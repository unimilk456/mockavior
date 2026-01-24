package com.mockavior.app.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockavior.app.admin.dto.kafka.KafkaDecodeMode;
import com.mockavior.app.admin.dto.kafka.KafkaMessageDTO;
import com.mockavior.app.admin.dto.kafka.KafkaPollPeekResponse;
import com.mockavior.app.admin.dto.kafka.KafkaValueDTO;
import com.mockavior.contract.payload.ResolvedBody;
import com.mockavior.kafka.model.KafkaMessage;
import com.mockavior.kafka.runtime.InMemoryKafkaStore;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    private final ObjectMapper objectMapper;

    /**
     * Peek messages (non-destructive).
     */
    @GetMapping("/{topic}")
    public ResponseEntity<KafkaPollPeekResponse> peek(
            @PathVariable String topic,
            @RequestParam(name = "decode", required = false) String decode
    ) {
        KafkaDecodeMode mode = KafkaDecodeMode.from(decode);

        log.debug("ADMIN → Kafka peek requested: topic={}, decode={}", topic, mode.wireValue());

        List<KafkaMessage> runtimeMessages = store.peek(topic);

        List<KafkaMessageDTO> messages = runtimeMessages.stream()
                .map(m -> toDto(m, mode))
                .toList();

        log.debug("ADMIN ← Kafka peek result: topic={}, count={}, decode={}", topic, messages.size(), mode.wireValue());

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
    public ResponseEntity<KafkaMessageDTO> take(
            @PathVariable String topic,
            @RequestParam(name = "decode", required = false) String decode
    ) {
        KafkaDecodeMode mode = KafkaDecodeMode.from(decode);
        log.debug("ADMIN → Kafka take requested: topic={}, decode={}", topic, mode.wireValue());

        Optional<KafkaMessage> message = store.take(topic);

        if (message.isPresent()) {
            KafkaMessageDTO dto = toDto(message.get(), mode);

            log.debug("ADMIN ← Kafka take success: topic={}, key={}, decode={}", dto.topic(), dto.key(), mode.wireValue());

            return ResponseEntity.ok(dto);
        }

        log.debug("ADMIN ← Kafka take empty: topic={}, decode={}", topic, mode.wireValue());

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

    private KafkaMessageDTO toDto(KafkaMessage message, KafkaDecodeMode decodeMode) {

        ResolvedBody body = message.value();
        byte[] bytes = body.bytes();

        String rawBase64 = Base64.getEncoder().encodeToString(bytes);

        Object decoded = null;

        switch (decodeMode) {
            case TEXT -> decoded = new String(bytes, StandardCharsets.UTF_8);

            case JSON -> {
                try {
                    decoded = objectMapper.readTree(bytes);
                } catch (Exception ex) {
                    // intentionally swallow; keep decoded = null
                    log.debug("Kafka message JSON decode failed: topic={}, key={}, bytes={}", message.topic(), message.key(), bytes.length);
                }
            }

            case NONE -> {
                // leave decoded null
            }
        }

        return new KafkaMessageDTO(
                message.topic(),
                message.key(),
                new KafkaValueDTO(
                        rawBase64,
                        decoded,
                        decodeMode.wireValue(),
                        body.source().name().toLowerCase()
                )
        );
    }

}
