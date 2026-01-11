package com.mockavior.routing.when;

import com.mockavior.contract.model.RawWhen;
import com.mockavior.core.request.GenericRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Simple conditional matcher for routes.
 *
 * MVP:
 * - supports presence check for query params
 * - example: active: "*"
 */
@Slf4j
public final class WhenCondition {

    private final Map<String, Object> requiredQuery;
    private final Map<String, Object> requiredHeaders;

    public WhenCondition(Map<String, Object> requiredQuery, Map<String, Object> requiredHeaders) {
        this.requiredQuery = requiredQuery == null ? Map.of() : Map.copyOf(requiredQuery);
        this.requiredHeaders = normalizeHeaderKeys(requiredHeaders);
    }

    public static WhenCondition alwaysTrue() {
        return new WhenCondition(Map.of(), Map.of());
    }

    public static WhenCondition fromRaw(RawWhen raw) {
        if (raw == null) {
            return alwaysTrue();
        }
        return new WhenCondition(raw.query(), raw.headers());
    }

    public boolean matches(GenericRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        boolean queryOk = matchesQuery(request);
        boolean headersOk = matchesHeaders(request);

        if (log.isTraceEnabled()) {
            log.trace(
                    "WhenCondition evaluated: queryOk={}, headersOk={}, condition={}",
                    queryOk,
                    headersOk,
                    this
            );
        }

        return queryOk && headersOk;
    }

    private boolean matchesQuery(GenericRequest request) {
        if (requiredQuery.isEmpty()) {
            return true;
        }

        Object queryObj = request.metadata("query");
        if (!(queryObj instanceof Map<?, ?> query)) {
            log.debug("WhenCondition query failed: request has no query params");
            return false;
        }

        for (Map.Entry<String, Object> e : requiredQuery.entrySet()) {
            String key = e.getKey();
            Object rule = e.getValue();

            Object valueObj = query.get(key);
            if (!(valueObj instanceof List<?> values) || values.isEmpty()) {
                log.trace("WhenCondition query param missing: {}", key);
                return false;
            }

            Object actual = values.get(0);

            if ("*".equals(rule)) {
                continue;
            }

            if (!String.valueOf(actual).equalsIgnoreCase(String.valueOf(rule))) {
                log.trace(
                        "WhenCondition query param mismatch: {} expected={}, actual={}",
                        key,
                        rule,
                        actual
                );
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean matchesHeaders(GenericRequest request) {
        if (requiredHeaders.isEmpty()) {
            return true;
        }

        Object headersObj = request.metadata("headers");
        if (!(headersObj instanceof Map<?, ?> headers)) {
            log.debug("WhenCondition headers failed: request has no headers");
            return false;
        }

        for (Map.Entry<String, Object> e : requiredHeaders.entrySet()) {
            String key = e.getKey();
            Object rule = e.getValue();

            Object actual = headers.get(key);
            if (actual == null) {
                log.trace("WhenCondition header missing: {}", key);
                return false;
            }

            if ("*".equals(rule)) {
                continue;
            }

            if (!String.valueOf(actual).equalsIgnoreCase(String.valueOf(rule))) {
                log.trace(
                        "WhenCondition header mismatch: {} expected={}, actual={}",
                        key,
                        rule,
                        actual
                );
                return false;
            }
        }

        return true;
    }

    private static Map<String, Object> normalizeHeaderKeys(Map<String, Object> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> e : headers.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String key = e.getKey().toLowerCase(Locale.ROOT);
            normalized.put(key, e.getValue());
        }
        return Map.copyOf(normalized);
    }

    @Override
    public String toString() {
        return "WhenCondition{query=" + requiredQuery + '}';
    }
}
