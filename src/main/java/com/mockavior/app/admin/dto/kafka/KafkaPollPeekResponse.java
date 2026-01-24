package com.mockavior.app.admin.dto.kafka;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        description = "Kafka topic peek response (non-destructive)",
        example = """
    {
      "topic": "user.created",
      "count": 3,
      "messages": [
        { "key": "1", "value": "{...}", "topic": "user.created" }
      ]
    }
    """
)
public record KafkaPollPeekResponse(

        @Schema(description = "Kafka topic name", example = "user.created")
        String topic,

        @Schema(description = "Number of messages in topic", example = "3")
        int count,

        @Schema(description = "Messages currently stored in topic")
        List<KafkaMessageDTO> messages
) {}
