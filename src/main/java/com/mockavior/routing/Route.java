package com.mockavior.routing;

import com.mockavior.behavior.Behavior;
import com.mockavior.routing.when.WhenCondition;

import java.util.Objects;

/**
 * A compiled runtime rule: matcher + behavior.
 * If matcher matches, behavior is applied.
 */
public final class Route {

    private final RouteMatcher matcher;
    private final Behavior behavior;
    private final int priority;
    private final String id;
    private final WhenCondition when;

    /**
     * @param matcher route matcher
     * @param behavior behavior to apply when matched
     * @param priority higher value means higher priority
     */
    public Route(RouteMatcher matcher, Behavior behavior, int priority, String id, WhenCondition when) {
        this.matcher = Objects.requireNonNull(matcher, "matcher must not be null");
        this.behavior = Objects.requireNonNull(behavior, "behavior must not be null");
        this.priority = priority;
        this.id = (id == null || id.isBlank()) ? null : id.trim();
        this.when = when;

    }

    public RouteMatcher matcher() {
        return matcher;
    }

    public Behavior behavior() {
        return behavior;
    }

    public int priority() {
        return priority;
    }

    public String id() {
        return id;
    }

    public WhenCondition when() {
        return when;
    }

    @Override
    public String toString() {
        return "Route{" +
                "matcher=" + matcher +
                ", behavior=" + behavior +
                ", priority=" + priority +
                ", id='" + id + '\'' +
                '}';
    }
}
