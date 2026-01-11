package com.mockavior.behavior;

import com.mockavior.core.request.GenericRequest;
import com.mockavior.routing.MatchResult;

/**
 * Behavior applied when a route is matched.
 * Must be thread-safe and stateless.
 */
@FunctionalInterface
public interface Behavior {

    /**
     * Apply behavior to the incoming request.
     *
     * @param request incoming generic request
     * @param match match result with extracted params
     * @return behavior result
     */
    BehaviorResult apply(GenericRequest request, MatchResult match);
}
