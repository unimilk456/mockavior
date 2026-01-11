package com.mockavior.routing;

import java.util.Objects;

/**
 * RouteMatch is a pairing of matched Route and the corresponding MatchResult (with extracted params).
 */
public final class RouteMatch {

    private final Route route;
    private final MatchResult matchResult;

    public RouteMatch(Route route, MatchResult matchResult) {
        this.route = Objects.requireNonNull(route, "route must not be null");
        this.matchResult = Objects.requireNonNull(matchResult, "matchResult must not be null");
    }

    public Route route() {
        return route;
    }

    public MatchResult matchResult() {
        return matchResult;
    }

    @Override
    public String toString() {
        return "RouteMatch{" +
                "route=" + route +
                ", matchResult=" + matchResult +
                '}';
    }
}
