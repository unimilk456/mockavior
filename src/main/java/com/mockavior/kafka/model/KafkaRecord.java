package com.mockavior.kafka.model;

public record KafkaRecord(
        String topic,
        KafkaMessage message
) {}
