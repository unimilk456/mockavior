package com.mockavior.transport.http;

import com.mockavior.core.request.GenericRequest;
import com.mockavior.core.request.Protocol;
import com.mockavior.routing.MatchResult;
import com.mockavior.routing.RouteMatcher;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

/**
 * HTTP route matcher based on method + path pattern.
 */
@Slf4j
public final class HttpRouteMatcher implements RouteMatcher {

    private final HttpMethod method;
    private final HttpPathPattern pathPattern;

    public HttpRouteMatcher(HttpMethod method, String pathTemplate) {
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.pathPattern = HttpPathPattern.compile(
                Objects.requireNonNull(pathTemplate, "pathTemplate must not be null")
        );

        log.debug(
                "Created HttpRouteMatcher: method={}, pathTemplate={}",
                method,
                pathTemplate
        );
    }

    @Override
    public MatchResult match(GenericRequest request) {
        if (request.protocol() != Protocol.HTTP) {

            log.trace(
                    "Protocol mismatch: expected HTTP, actual={}",
                    request.protocol()
            );

            return MatchResult.noMatch();
        }

        Object m = request.metadata("method");
        if (!(m instanceof HttpMethod) || m != method) {

            log.trace(
                    "HTTP method mismatch: expected={}, actual={}",
                    method,
                    m
            );

            return MatchResult.noMatch();
        }

        Map<String, Object> params =
                pathPattern.match(request.operation());

        if (params == null) {
            log.trace(
                    "Path mismatch: method={}, path={}",
                    method,
                    request.operation()
            );

            return MatchResult.noMatch();
        }

        log.trace(
                "Route matched: method={}, path={}, params={}",
                method,
                request.operation(),
                params
        );

        return MatchResult.matched(params);
    }

    @Override
    public String toString() {
        return "HttpRouteMatcher{" +
                "method=" + method +
                ", pathPattern=" + pathPattern +
                '}';
    }
}
