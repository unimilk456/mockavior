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

        for (Map.Entry<String, Object> entry : requiredQuery.entrySet()) {
            String key = entry.getKey();
            Object rule = entry.getValue();

            List<String> values = extractQueryValues(query, key);
            if (values.isEmpty() || !matchesRule(key, rule, values)) {
                return false;
            }
        }

        return true;
    }

    private List<String> extractQueryValues(Map<?, ?> query, String key) {
        Object valueObj = query.get(key);

        if (!(valueObj instanceof List<?> rawValues) || rawValues.isEmpty()) {
            log.trace("WhenCondition query param missing: {}", key);
            return List.of();
        }

        return rawValues.stream()
                .map(String::valueOf)
                .toList();
    }


    private boolean matchesRule(String key, Object rule, List<String> values) {

        if ("*".equals(rule)) {
            return true;
        }

        if (rule instanceof String expected) {
            return matchesAny(values, List.of(expected));
        }

        if (rule instanceof List<?> listRule) {
            return matchesAny(values, toStringList(listRule));
        }

        if (rule instanceof Map<?, ?> mapRule) {
            return matchesAnyAndAll(key, values, mapRule);
        }

        throw new IllegalArgumentException(
                "Unsupported when.query rule for key '" + key + "': " + rule
        );
    }

    private boolean matchesAnyAndAll(
            String key,
            List<String> values,
            Map<?, ?> rule
    ) {
        if (rule.containsKey("any")) {
            List<String> expectedAny = extractList(rule, "any", key);
            if (!matchesAny(values, expectedAny)) {
                return false;
            }
        }

        if (rule.containsKey("all")) {
            List<String> expectedAll = extractList(rule, "all", key);
            return matchesAll(values, expectedAll);
        }

        return true;
    }

    private boolean matchesAny(List<String> values, List<String> expected) {
        return values.stream()
                .anyMatch(v ->
                        expected.stream()
                                .anyMatch(v::equalsIgnoreCase)
                );
    }

    private boolean matchesAll(List<String> values, List<String> expected) {
        return expected.stream()
                .allMatch(exp ->
                        values.stream()
                                .anyMatch(v -> v.equalsIgnoreCase(exp))
                );
    }

    private List<String> extractList(Map<?, ?> rule, String key, String param) {
        Object obj = rule.get(key);

        if (!(obj instanceof List<?> list)) {
            throw new IllegalArgumentException(
                    "when.query." + param + "." + key + " must be a list"
            );
        }

        return toStringList(list);
    }

    private List<String> toStringList(List<?> list) {
        return list.stream()
                .map(String::valueOf)
                .toList();
    }


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
