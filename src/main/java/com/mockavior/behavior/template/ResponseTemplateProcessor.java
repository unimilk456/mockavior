package com.mockavior.behavior.template;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies template parameters to response body.
 *
 * Supported body types:
 * - String
 * - Map (processed recursively)
 * - List (processed recursively)
 *
 * Unsupported types are returned as-is.
 *
 * Missing parameters are not replaced.
 */
@Slf4j
public final class ResponseTemplateProcessor {

    private ResponseTemplateProcessor() {
    }

    public static Object apply(Object body, Map<String, ?> params) {
        if (body == null || params == null  || params.isEmpty()) {
            log.trace("Template processing skipped (body={}, paramsEmpty={})",
                    body == null ? "null" : body.getClass().getSimpleName(),
                    params == null || params.isEmpty()
            );
            return body;
        }

        log.trace("Applying response template: bodyType={}, paramsCount={}",
                body.getClass().getSimpleName(),
                params.size()
        );

        if (body instanceof String s) {
            return applyToString(s, params);
        }

        if (body instanceof Map<?, ?> map) {
            return applyToMap(map, params);
        }

        if (body instanceof List<?> list) {
            return applyToList(list, params);
        }

        log.trace("Template processing skipped for unsupported body type: {}", body.getClass().getName());

        return body;
    }

    private static String applyToString(String s, Map<String, ?> params) {
        String result = s;
        for (Map.Entry<String, ?> e : params.entrySet()) {
            result = result.replace(
                    "{" + e.getKey() + "}",
                    String.valueOf(e.getValue())
            );
        }
        return result;
    }

    private static Map<String, Object> applyToMap(
            Map<?, ?> map,
            Map<String, ?> params
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            result.put(
                    String.valueOf(e.getKey()),
                    apply(e.getValue(), params)
            );
        }
        return result;
    }

    private static List<Object> applyToList(
            List<?> list,
            Map<String, ?> params
    ) {
        List<Object> result = new ArrayList<>();
        for (Object o : list) {
            result.add(apply(o, params));
        }
        return result;
    }
}
