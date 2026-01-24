package com.mockavior.app.admin.dto.kafka;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for exposing Kafka messages via Admin API.
 * Runtime-only fields (delay, repeat, etc.) are intentionally hidden.
 */
@Schema(description = "Kafka message DTO for admin inspection")
public record KafkaMessageDTO(

        @Schema(description = "Kafka topic name", example = "user.created")
        String topic,

        @Schema(description = "Message key", example = "user-1")
        String key,

        @Schema(description = "Message payload")
        Object value
) {
}
