package com.mockavior.contract.model;

import java.util.Map;

public record Proxy(String baseUrl) {

    public static Proxy fromMap(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        Object baseUrlObj = data.get("baseUrl");

        if (!(baseUrlObj instanceof String baseUrl) || baseUrl.isBlank()) {
            throw new IllegalArgumentException("proxy.baseUrl must be a non-empty string");
        }

        return new Proxy(baseUrl);
    }
}

