package com.mockavior.contract.model;

import java.util.Map;
import java.util.Objects;

public record Settings(
        Mode mode,
        int defaultStatus,
        Proxy proxy
) {

    public Settings {
        // дефолты и защита от null при прямом создании
        mode = (mode == null) ? Mode.STRICT : mode;
    }

    @SuppressWarnings("unchecked")
    public static Settings fromMap(Map<String, Object> data) {
        Objects.requireNonNull(data, "'settings' section must not be null");

        Mode mode = parseMode(data.get("mode"));
        int defaultStatus = parseStatus(data.getOrDefault("defaultStatus", 404));
        Proxy proxy = parseProxy(data.get("proxy"));

        return new Settings(mode, defaultStatus, proxy);

    }

    private static Mode parseMode(Object value) {
        if (value == null) {
            return Mode.STRICT;
        }

        try {
            return Mode.valueOf(value.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid settings.mode value: " + value +
                            " (allowed: STRICT, PASSTHROUGH)",
                    e
            );
        }
    }

    private static int parseStatus(Object value) {
        int status = value instanceof Number number
                ? number.intValue()
                : Integer.parseInt(value.toString());

        if (status < 100 || status > 599) {
            throw new IllegalArgumentException(
                    "Invalid settings.defaultStatus: " + status +
                            " (must be between 100 and 599)"
            );
        }

        return status;
    }

    @SuppressWarnings("unchecked")
    private static Proxy parseProxy(Object value) {
        if (value instanceof Map<?, ?> map) {
            return Proxy.fromMap((Map<String, Object>) map);
        }
        return null;
    }
}

