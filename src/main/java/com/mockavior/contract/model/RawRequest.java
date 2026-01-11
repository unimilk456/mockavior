package com.mockavior.contract.model;

import java.util.Map;
import java.util.Objects;

public record RawRequest(
        String protocol,
        String method,
        String path
) {

    public RawRequest {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException(
                    "request.method must be specified"
            );
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "request.path must be specified"
            );
        }

        protocol = (protocol == null || protocol.isBlank())
                ? "http"
                : protocol.toLowerCase();

        method = method.toUpperCase();
    }

    public static RawRequest fromMap(Map<String, Object> data) {
        Objects.requireNonNull(data, "request section must not be null");

        String protocol = (String) data.getOrDefault("protocol", "http");
        String method = (String) data.get("method");
        String path = (String) data.get("path");

        return new RawRequest(protocol, method, path);
    }
}

