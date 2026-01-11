package com.mockavior.contract.model;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

@Slf4j
public final class RawWhen {

    private final Map<String, Object> query;
    private final Map<String, Object> headers;

    public RawWhen(Map<String, Object> query, Map<String, Object> headers) {
        this.query = query == null ? Map.of() : Map.copyOf(query);
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public Map<String, Object> query() {
        return query;
    }

    public Map<String, Object> headers() {
        return headers;
    }

    @SuppressWarnings("unchecked")
    public static RawWhen fromMap(Map<String, Object> data) {

        Objects.requireNonNull(data, "'when' section must not be null");

        log.debug("Parsing 'when' section: keys={}", data.keySet());

        Map<String, Object> query = extractMap(data.get("query"));
        Map<String, Object> headers = extractMap(data.get("headers"));

        if (log.isTraceEnabled()) {
            log.trace("Parsed when.query={}", query);
            log.trace("Parsed when.headers={}", headers);
        }

        return new RawWhen(query, headers);
    }

    private static Map<String, Object> extractMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) map;
            return result;
        }
        return Map.of();
    }
}
