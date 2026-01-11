package com.mockavior.core.engine;

import com.mockavior.behavior.Behavior;
import com.mockavior.behavior.BehaviorResult;
import com.mockavior.core.request.GenericRequest;
import com.mockavior.core.snapshot.ContractSnapshot;
import com.mockavior.routing.MatchResult;
import com.mockavior.routing.RouteMatch;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

/**
 * Core execution engine.
 * Responsibilities:
 * - Route request using snapshot router
 * - Apply matched behavior
 * - Apply fallback behavior if no route matches
 */
@Slf4j
public final class BehaviorEngine {

    public EngineResult handle(ContractSnapshot snapshot,
                               GenericRequest request,
                               Behavior fallbackBehavior) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(fallbackBehavior, "fallbackBehavior must not be null");

        log.debug(
                "Handling request: snapshotVersion={}, method={}, operation={}",
                snapshot.version().value(),
                request.metadata("method"),
                request.operation()
        );

        Optional<RouteMatch> routeMatch = snapshot.router().find(request);

        if (routeMatch.isPresent()) {
            RouteMatch rm = routeMatch.get();
            String routeId = rm.route().id();

            log.debug(
                    "Route matched: snapshotVersion={}, routeId={}",
                    snapshot.version().value(),
                    rm.route().id()
            );

            log.trace(
                    "Applying behavior: routeId={}, behavior={}",
                    rm.route().id(),
                    rm.route().behavior().getClass().getSimpleName()
            );

            BehaviorResult result =
                    rm.route().behavior().apply(request, rm.matchResult());

            return new EngineResult(snapshot, result, routeId);
        }

        log.info(
                "No route matched, applying fallback: snapshotVersion={}, method={}, operation={}",
                snapshot.version().value(),
                request.metadata("method"),
                request.operation()
        );
        // 🔴 Fallback is applied ONLY when no route matches
        BehaviorResult fallbackResult =
                fallbackBehavior.apply(request, MatchResult.noMatch());

        return new EngineResult(snapshot, fallbackResult, null);
    }
}
