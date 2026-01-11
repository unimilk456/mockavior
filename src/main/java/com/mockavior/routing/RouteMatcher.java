package com.mockavior.routing;

import com.mockavior.core.request.GenericRequest;

/**
 * Matches an incoming request against a route rule.
 * Must be thread-safe.
 */
@FunctionalInterface
public interface RouteMatcher {

    /**
     * @param request incoming request (protocol-agnostic)
     * @return MatchResult.noMatch() if not matched; otherwise MatchResult.matched(params).
     */
    MatchResult match(GenericRequest request);
}
