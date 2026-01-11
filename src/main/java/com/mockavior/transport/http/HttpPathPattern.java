package com.mockavior.transport.http;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiled HTTP path pattern with named parameters.
 * Example:
 *   /users/{id}  ->  ^/users/(?<id>[^/]+)$
 */
@Slf4j
public final class HttpPathPattern {

    private static final Pattern PARAM_PATTERN =
            Pattern.compile("\\{([^/}]+)}");

    private final Pattern regex;
    private final List<String> paramNames;

    private HttpPathPattern(Pattern regex, List<String> paramNames) {
        this.regex = regex;
        this.paramNames = paramNames;
    }

    public static HttpPathPattern compile(String template) {
        log.debug("Compiling HTTP path pattern from template: {}", template);

        Matcher matcher = PARAM_PATTERN.matcher(template);

        StringBuilder regexBuilder = new StringBuilder();
        List<String> params = new ArrayList<>();

        int lastEnd = 0;

        while (matcher.find()) {
            regexBuilder.append(
                    Pattern.quote(template.substring(lastEnd, matcher.start()))
            );

            String paramName = matcher.group(1);
            params.add(paramName);

            // named capturing group
            regexBuilder.append("(?<").append(paramName).append(">[^/]+)");

            lastEnd = matcher.end();
        }

        // tail after last param
        regexBuilder.append(
                Pattern.quote(template.substring(lastEnd))
        );

        Pattern finalPattern =
                Pattern.compile("^" + regexBuilder + "$");

        HttpPathPattern compiled =
                new HttpPathPattern(finalPattern, List.copyOf(params));

        log.debug(
                "Compiled HTTP path pattern: template='{}', regex='{}', params={}",
                template,
                finalPattern,
                params
        );

        return compiled;
    }

    /**
     * @param path incoming HTTP path
     * @return map of extracted params if matched, otherwise null
     */
    public Map<String, Object> match(String path) {
        log.trace("Matching path '{}' against pattern '{}'", path, regex);

        Matcher matcher = regex.matcher(path);

        log.trace("Path '{}' did not match pattern '{}'", path, regex);

        if (!matcher.matches()) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        for (String name : paramNames) {
            result.put(name, matcher.group(name));
        }

        log.trace("Path '{}' matched with params {}", path, result);

        return result;
    }

    @Override
    public String toString() {
        return "HttpPathPattern{" +
                "regex=" + regex +
                ", paramNames=" + paramNames +
                '}';
    }
}
