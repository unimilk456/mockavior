package com.mockavior.core.request;

import java.util.Map;
import java.util.Objects;

/**
 * Protocol-agnostic representation of an outgoing response produced by the engine.
 * Transport adapters translate it into native protocol responses.
 */
public final class GenericResponse {

    private final Object payload;
    private final Map<String, Object> metadata;

    private GenericResponse(Object payload, Map<String, Object> metadata) {
        this.payload = payload;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static GenericResponse of(Object payload, Map<String, Object> metadata) {
        return new GenericResponse(payload, metadata);
    }

    public static GenericResponse of(Object payload) {
        return new GenericResponse(payload, Map.of());
    }

    public Object payload() {
        return payload;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public Object metadata(String key) {
        return metadata.get(key);
    }

    @Override
    public String toString() {
        return "GenericResponse{" +
                "payload=" + (payload == null ? "null" : payload.getClass().getSimpleName()) +
                ", metadataKeys=" + metadata.keySet() +
                '}';
    }
}
