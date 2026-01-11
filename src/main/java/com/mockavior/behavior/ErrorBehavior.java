package com.mockavior.behavior;

import com.mockavior.core.request.GenericRequest;
import com.mockavior.routing.MatchResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Error behavior.
 * Always terminates request processing and returns an error response
 * with the configured HTTP status code.
 * This behavior:
 * - does NOT proxy
 * - does NOT return a body
 * - is deterministic
 */
@Slf4j
public final class ErrorBehavior implements Behavior {

    private final int statusCode;

    public ErrorBehavior(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public BehaviorResult apply(GenericRequest request, MatchResult match) {
        log.debug(
                "Applying ErrorBehavior: status={}, matchedRoute={}",
                statusCode,
                match != null && match.matched()
        );

        return BehaviorResult.error(statusCode);
    }

    @Override
    public String toString() {
        return "ErrorBehavior{" +
                "statusCode=" + statusCode +
                '}';
    }
}
