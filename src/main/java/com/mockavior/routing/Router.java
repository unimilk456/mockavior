package com.mockavior.routing;

import com.mockavior.core.request.GenericRequest;

import java.util.Optional;

/**
 * Selects the first matching route for a given request.
 * Implementations must be thread-safe.
 */
public interface Router {

    Optional<RouteMatch> find(GenericRequest request);
}
