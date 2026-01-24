package com.mockavior.contract.model;

import com.mockavior.behavior.delay.DelaySpec;
import com.mockavior.behavior.delay.RandomDelay;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record RawResponse(
        String type,
        int status,
        Map<String, Object> headers,
        Object body,
        Object bodyFile,
        DelaySpec delay
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
        delay = delay == null
                ? new DelaySpec(Duration.ZERO, null)
                : delay;

        if (bodyFile != null && !(bodyFile instanceof String)) {
            throw new IllegalArgumentException(
                    "response.bodyFile must be a string path relative to contract directory"
            );
        }
    }

    @SuppressWarnings("unchecked")
    public static RawResponse fromMap(Map<String, Object> data) {
        Objects.requireNonNull(data, "response section must not be null");

        String type = (String) data.get("type");

        int status = parseStatus(data.getOrDefault("status", 200));

        DelaySpec delay = parseDelay(data.get("delay"));

        Object body = data.get("body");

        Object bodyFile = data.get("bodyFile");

        Map<String, Object> headers = null;
        Object headersObj = data.get("headers");
        if (headersObj instanceof Map<?, ?>) {
            headers = (Map<String, Object>) headersObj;
        }

        return new RawResponse(type, status, headers, body, bodyFile, delay);
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

    private static DelaySpec parseDelay(Object value) {
        return switch (value) {
            case null -> null;

            case Number number ->
                    new DelaySpec(
                            Duration.ofMillis(number.longValue()),
                            null
                    );

            case String str ->
                    new DelaySpec(
                            Duration.parse("PT" + str.toUpperCase()),
                            null
                    );

            // new format:
            // delay:
            //   fixed: 200ms
            //   random:
            //     min: 50ms
            //     max: 300ms

            case Map<?, ?> map -> {
                Duration fixed = null;
                RandomDelay random = null;

                Object fixedRaw = map.get("fixed");
                if (fixedRaw != null) {
                    fixed = parseDuration(fixedRaw);
                }

                Object randomRaw = map.get("random");
                if (randomRaw instanceof Map<?, ?> rnd) {
                    Duration min = parseDuration(rnd.get("min"));
                    Duration max = parseDuration(rnd.get("max"));
                    random = new RandomDelay(min, max);
                }

                yield new DelaySpec(fixed, random);
            }

            default ->
                    throw new IllegalArgumentException(
                            "response.delay must be number, string duration or delay object, got: " + value
                    );
        };
    }

    private static Duration parseDuration(Object raw) {
        if (raw instanceof Number number) {
            return Duration.ofMillis(number.longValue());
        }
        if (raw instanceof String str) {
            return Duration.parse("PT" + str.toUpperCase());
        }
        throw new IllegalArgumentException("Unsupported duration value: " + raw);
    }

}

