package com.mockavior.behavior;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

/**
 * Result of behavior resolution by runtime engine.
 * payload:
 * - String, Map, List, or null
 * metadata:
 * - "status"  -> HTTP status code
 * - "headers" -> Map<String, Object>
 */
@Slf4j
public final class BehaviorResult {

    private final BehaviorType type;
    private final Object payload;
    private final Map<String, Object> metadata;

    public static final String META_STATUS = "status";
    public static final String META_HEADERS = "headers";

    private BehaviorResult(
            BehaviorType type,
            Object payload,
            Map<String, Object> metadata
    ) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.payload = payload;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);

        log.trace(
                "BehaviorResult created: type={}, payloadPresent={}, metadataKeys={}",
                type,
                payload != null,
                this.metadata.keySet()
        );
    }

    // ---------------------------------------------------------
    // Factory methods
    // ---------------------------------------------------------

    public static BehaviorResult mock(
            Object payload,
            int status,
            Map<String, Object> headers
    ) {
        log.debug(
                "Creating MOCK BehaviorResult (status={}, headersCount={}, payloadPresent={})",
                status,
                headers == null ? 0 : headers.size(),
                payload != null
        );
        return new BehaviorResult(
                BehaviorType.MOCK,
                payload,
                Map.of(
                        META_STATUS, status,
                        META_HEADERS, headers == null ? Map.of() : Map.copyOf(headers)
                )

        );
    }

    public static BehaviorResult error(int status) {
        log.debug("Creating ERROR BehaviorResult (status={})", status);

        return new BehaviorResult(
                BehaviorType.ERROR,
                null,
                Map.of("status", status)
        );
    }

    public static BehaviorResult proxy() {
        log.debug("Creating PROXY BehaviorResult");

        return new BehaviorResult(
                BehaviorType.PROXY,
                null,
                Map.of()
        );
    }

    // ---------------------------------------------------------

    public BehaviorType type() {
        return type;
    }

    public Object payload() {
        return payload;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }
}
