package com.mockavior.routing;

import com.mockavior.core.request.GenericRequest;
import com.mockavior.routing.when.WhenCondition;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default router:
 * - sorts routes by priority descending
 * - iterates and returns first matched route
 */
@Slf4j
public final class DefaultRouter implements Router {

    private final List<Route> routes;

    public DefaultRouter(List<Route> routes) {
        Objects.requireNonNull(routes, "routes must not be null");
        this.routes = routes.stream()
                .sorted(Comparator.comparingInt(Route::priority).reversed())
                .toList();
    }

    @Override
    public Optional<RouteMatch> find(GenericRequest request) {
        log.debug(
                "Routing request: method={}, operation={}, routes={}",
                request.metadata("method"),
                request.operation(),
                routes.size()
        );

        for (Route route : routes) {
// 1️⃣ Path / method matching
            MatchResult mr = route.matcher().match(request);
            if (!mr.matched()) {
                log.trace(
                        "Route not matched by path/method: routeId={}",
                        route.id()
                );
                continue;
            }

            // 2️⃣ When-condition matching
            WhenCondition when = route.when();
            if (when != null && !when.matches(request)) {
                log.debug(
                        "Route matched by path but rejected by when-condition: routeId={}, when={}",
                        route.id(),
                        when
                );
                continue;
            }

            // 3️⃣ Merge params for behavior/template usage
            Map<String, Object> mergedParams = new HashMap<>();
            mergedParams.putAll(mr.params());     // {id}
            mergedParams.putAll(extractQueryParams(request));     // {active}

            log.debug(
                    "Route selected: routeId={}, params={}",
                    route.id(),
                    mergedParams
            );

            MatchResult enriched =
                    MatchResult.matched(mergedParams);

            return Optional.of(new RouteMatch(route, enriched));
        }

        // No route matched
        log.info(
                "No route matched request: method={}, operation={}",
                request.metadata("method"),
                request.operation()
        );
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractQueryParams(GenericRequest request) {
        Object queryObj = request.metadata().get("query");
        if (!(queryObj instanceof Map<?, ?> raw)) {
            return Map.of();
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            String key = String.valueOf(e.getKey());
            Object value = e.getValue();

            if (value instanceof List<?> list && !list.isEmpty()) {
                result.put(key, list.get(0));
            }
        }
        return result;
    }
}