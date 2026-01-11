package com.mockavior.contract.model;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record RawResponse(
        String type,
        int status,
        Map<String, Object> headers,
        Object body,
        Duration delay
) {

    public RawResponse {
        if (type == null) {
            throw new IllegalArgumentException("response.type must be specified");
        }

        if (status < 100 || status > 599) {
            throw new IllegalArgumentException(
                    "response.status must be a valid HTTP status code: " + status
            );
        }

        type = type.toLowerCase();
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        delay = delay == null ? Duration.ZERO : delay;
    }

    @SuppressWarnings("unchecked")
    public static RawResponse fromMap(Map<String, Object> data) {
        Objects.requireNonNull(data, "response section must not be null");

        String type = (String) data.get("type");

        int status = parseStatus(data.getOrDefault("status", 200));

        Duration delay = parseDelay(data.get("delay"));

        Object body = data.get("body");

        Map<String, Object> headers = null;
        Object headersObj = data.get("headers");
        if (headersObj instanceof Map<?, ?>) {
            headers = (Map<String, Object>) headersObj;
        }

        return new RawResponse(type, status, headers, body, delay);
    }

    private static int parseStatus(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            return Integer.parseInt(s);
        }
        throw new IllegalArgumentException(
                "response.status must be a number or string, got: " + value
        );
    }

    private static Duration parseDelay(Object value) {
        if (value == null) {
            return Duration.ZERO;
        }
        if (value instanceof Number n) {
            return Duration.ofMillis(n.longValue());
        }
        if (value instanceof String s) {
            return Duration.parse("PT" + s.toUpperCase());
        }
        throw new IllegalArgumentException(
                "response.delay must be number (ms) or ISO-8601 duration, got: " + value
        );
    }
}

