package com.mockavior.core.request;

import java.util.Map;
import java.util.Objects;

/**
 * Protocol-agnostic representation of an incoming request.
 *
 * Notes:
 * - operation is a generic identifier: HTTP path, gRPC service.method, GraphQL op name, etc.
 * - metadata is a generic bag (method, headers, traceId, auth, etc.) interpreted by adapters/matchers.
 * - payload is protocol-specific body/message, kept as Object to avoid coupling core to serialization.
 */
public final class GenericRequest {

    private final Protocol protocol;
    private final String operation;
    private final Object payload;
    private final Map<String, Object> metadata;

    private GenericRequest(
            Protocol protocol,
            String operation,
            Object payload,
            Map<String, Object> metadata
    ) {
        this.protocol = Objects.requireNonNull(protocol, "protocol must not be null");
        this.operation = Objects.requireNonNull(operation, "operation must not be null");
        this.payload = payload;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static GenericRequest of(
            Protocol protocol,
            String operation,
            Object payload,
            Map<String, Object> metadata
    ) {
        return new GenericRequest(protocol, operation, payload, metadata);
    }

    /**
     * Convenience factory for requests with no payload.
     */
    public static GenericRequest of(
            Protocol protocol,
            String operation,
            Map<String, Object> metadata
    ) {
        return new GenericRequest(protocol, operation, null, metadata);
    }

    public Protocol protocol() {
        return protocol;
    }

    public String operation() {
        return operation;
    }

    public Object payload() {
        return payload;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    /**
     * Convenience accessor for metadata values.
     */
    public Object metadata(String key) {
        return metadata.get(key);
    }

    @Override
    public String toString() {
        return "GenericRequest{" +
                "protocol=" + protocol +
                ", operation='" + operation + '\'' +
                ", payload=" + (payload == null ? "null" : payload.getClass().getSimpleName()) +
                ", metadataKeys=" + metadata.keySet() +
                '}';
    }
}
