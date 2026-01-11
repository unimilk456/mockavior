package com.mockavior.routing;

import java.util.Map;
/**
 * Result of routing match attempt.
 * When matched=true, params contains extracted route parameters.
 */


public final class MatchResult {

    private static final MatchResult NO_MATCH =
            new MatchResult(false, Map.of());

    private final boolean matched;
    private final Map<String, Object> params;
//    private final boolean conditionsMatched;

    private MatchResult(boolean matched, Map<String, Object> params) {
        this.matched = matched;
        this.params = params;
//        this.conditionsMatched = conditionsMatched;
    }

    public static MatchResult matched(Map<String, Object> params) {
        return new MatchResult(true, params);
    }

    public static MatchResult noMatch() {
        return NO_MATCH;
    }

    public boolean matched() {
        return matched;
    }

    public Map<String, Object> params() {
        return params;
    }
}
