package com.mockavior.app.admin.dto.kafka;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kafka message value representation")
public record KafkaValueDTO(

        @Schema(description = "Raw value as Base64-encoded bytes")
        String raw,

        @Schema(description = "Decoded value (depends on decode mode)")
        Object decoded,

        @Schema(description = "Decoding applied: none | text | json")
        String decode,

        @Schema(description = "Value source: inline | file")
        String source
) {}
