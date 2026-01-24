package com.mockavior.contract.compiler;

import com.mockavior.behavior.Behavior;
import com.mockavior.behavior.ErrorBehavior;
import com.mockavior.behavior.MockBehavior;
import com.mockavior.behavior.ProxyBehavior;
import com.mockavior.behavior.delay.DelaySpec;
import com.mockavior.contract.model.CompiledContract;
import com.mockavior.contract.model.Mode;
import com.mockavior.contract.model.RawContract;
import com.mockavior.contract.model.RawEndpoint;
import com.mockavior.contract.model.RawRequest;
import com.mockavior.contract.model.RawResponse;
import com.mockavior.contract.model.Settings;
import com.mockavior.core.snapshot.ContractSnapshot;
import com.mockavior.core.snapshot.SnapshotVersion;
import com.mockavior.kafka.compiler.KafkaScenarioCompiler;
import com.mockavior.kafka.model.KafkaScenario;
import com.mockavior.routing.DefaultRouter;
import com.mockavior.routing.Route;
import com.mockavior.routing.Router;
import com.mockavior.routing.when.WhenCondition;
import com.mockavior.transport.http.HttpMethod;
import com.mockavior.transport.http.HttpRouteMatcher;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public final class ContractCompiler {

    private final Clock clock;
    private final KafkaScenarioCompiler kafkaScenarioCompiler;

    public ContractCompiler(Clock clock, KafkaScenarioCompiler kafkaScenarioCompiler) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.kafkaScenarioCompiler = kafkaScenarioCompiler;
    }

    public CompiledContract compile(RawContract raw) {
        Objects.requireNonNull(raw, "raw contract must not be null");

        log.info("Starting contract compilation");
        try {
            List<Route> routes = new ArrayList<>();
            Map<String, DelaySpec> routeDelays = new HashMap<>();

            for (RawEndpoint e : raw.endpoints()) {
                RawRequest r = e.request();
                RawResponse resp = e.response();

                HttpMethod method =
                        HttpMethod.valueOf(r.method().toUpperCase());

                HttpRouteMatcher matcher =
                        new HttpRouteMatcher(method, r.path());

                int priority = resolvePriority(e);

                Behavior behavior = resolveBehavior(resp);

                WhenCondition whenCondition =
                        WhenCondition.fromRaw(e.when());



                log.debug(
                        "Compiled endpoint: id={}, method={}, path={}, priority={}, behavior={}, when={}",
                        e.id(),
                        method,
                        r.path(),
                        priority,
                        behavior.getClass().getSimpleName(),
                        whenCondition
                );

                routes.add(
                        new Route(
                                matcher,
                                behavior,
                                priority,
                                e.id(),
                                whenCondition
                        )
                );

                if (e.id() != null) {
                    routeDelays.put(e.id(), resp.delay());
                }
            }

            Router router = new DefaultRouter(routes);

            Map<String, KafkaScenario> kafkaScenarios = Map.of();

            if (raw.kafka() != null) {
                log.info(
                        "Kafka section detected in contract, scenarios={}",
                        raw.kafka().scenarios().size()
                );

                kafkaScenarios = kafkaScenarioCompiler.compileAll(raw.kafka());
            }


            ContractSnapshot.Settings snapshotSettings =
                    normalizeSettings(raw.settings());

            ContractSnapshot snapshot = new ContractSnapshot(
                    SnapshotVersion.next(),
                    Instant.now(clock),
                    router,
                    snapshotSettings,
                    routeDelays,
                    kafkaScenarios
            );

            Behavior fallbackBehavior = resolveFallback(snapshotSettings);

            log.info(
                    "Contract compiled successfully: snapshotVersion={}, routes={}, fallback={}",
                    snapshot.version().value(),
                    routes.size(),
                    fallbackBehavior.getClass().getSimpleName()
            );

            return new CompiledContract(snapshot, fallbackBehavior);

        } catch (Exception e) {
            log.error("Contract compilation failed", e);
            throw e;
        }
    }

    private int resolvePriority(RawEndpoint e) {
        if (e.priority() != null) {
            return e.priority();
        }
        return e.request().path().contains("{") ? 0 : 10;
    }

    private Behavior resolveBehavior(
            RawResponse r
    ) {
        return switch (r.type().toLowerCase()) {
            case "mock" ->
                    new MockBehavior(r.body(), r.status(), r.headers());
            case "proxy" ->
                    new ProxyBehavior();
            case "error" ->
                    new ErrorBehavior(r.status());
            default -> {
                log.error("Unknown response.type: {}", r.type());
                throw new IllegalArgumentException(
                        "Unknown response.type: " + r.type()
                );
            }
        };
    }

    private Behavior resolveFallback(
            ContractSnapshot.Settings settings
    ) {
        return switch (settings.mode()) {
            case STRICT ->
                    new ErrorBehavior(settings.defaultStatus());
            case PASSTHROUGH ->
                    new ProxyBehavior();
        };
    }

    private ContractSnapshot.Settings normalizeSettings(Settings raw) {

        if (raw == null) {
            return new ContractSnapshot.Settings(
                    Mode.STRICT,
                    404,
                    null
            );
        }

        URI proxyUri = null;
        if (raw.proxy() != null && raw.proxy().baseUrl() != null) {
            proxyUri = URI.create(raw.proxy().baseUrl());
        }

        return new ContractSnapshot.Settings(
                raw.mode() != null ? raw.mode() : Mode.STRICT,
                raw.defaultStatus(),
                proxyUri
        );
    }
}
