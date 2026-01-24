package com.mockavior.app.admin.dto.kafka;

public enum KafkaDecodeMode {
    NONE,
    TEXT,
    JSON;

    public static KafkaDecodeMode from(String raw) {
        if (raw == null) {
            return NONE;
        }
        return switch (raw.toLowerCase()) {
            case "text" -> TEXT;
            case "json" -> JSON;
            default -> NONE;
        };
    }

    public String wireValue() {
        return name().toLowerCase();
    }
}

