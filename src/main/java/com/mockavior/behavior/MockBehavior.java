package com.mockavior.behavior;

import com.mockavior.behavior.template.ResponseTemplateProcessor;
import com.mockavior.core.request.GenericRequest;
import com.mockavior.routing.MatchResult;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock behavior that returns a static or templated response.
 * Supported template parameters:
 * - path variables (e.g. /users/{id})
 * - query parameters (?limit=10)
 * Templates are applied recursively to:
 * - response body
 * - response headers
 */
@Slf4j
public final class MockBehavior implements Behavior {

    private final Object body;
    private final int status;
    private final Map<String, Object> headers;

    public MockBehavior(Object body, int status, Map<String, Object> headers) {
        this.body = body;
        this.status = status;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    @Override
    public BehaviorResult apply(GenericRequest request, MatchResult match) {
        log.debug(
                "Applying MockBehavior: status={}, bodyPresent={}, headersPresent={}",
                status,
                body != null,
                headers != null && !headers.isEmpty()
        );

        Map<String, Object> templateParams = new HashMap<>();

        // path params (/users/{id})
        templateParams.putAll(match.params());

        Map<String, Object> queryParams = extractQueryParams(request);
        templateParams.putAll(queryParams);

        log.trace(
                "MockBehavior template params keys: {}",
                templateParams.keySet()
        );

        Object resolvedBody =
                ResponseTemplateProcessor.apply(body, templateParams);

        @SuppressWarnings("unchecked")
        Map<String, Object> resolvedHeaders =
                headers == null
                        ? Map.of()
                        : (Map<String, Object>)
                        ResponseTemplateProcessor.apply(headers, templateParams);

        log.trace(
                "MockBehavior resolved body type={}, resolvedHeadersKeys={}",
                resolvedBody == null ? "null" : resolvedBody.getClass().getSimpleName(),
                resolvedHeaders.keySet()
        );

        return BehaviorResult.mock(resolvedBody, status, resolvedHeaders);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractQueryParams(GenericRequest request) {
        Object queryObj = request.metadata("query");

        if (!(queryObj instanceof Map<?, ?> queryMap)) {
            return Map.of();
        }

        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<?, ?> e : queryMap.entrySet()) {
            Object value = e.getValue();

            if (value instanceof List<?> list) {
                if (list.size() == 1) {
                    result.put(String.valueOf(e.getKey()), list.get(0));
                } else {
                    result.put(String.valueOf(e.getKey()), list);
                }
            } else {
                result.put(String.valueOf(e.getKey()), value);
            }
        }

        return result;
    }
}
