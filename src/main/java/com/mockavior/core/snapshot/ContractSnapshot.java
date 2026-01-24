package com.mockavior.core.snapshot;

import com.mockavior.behavior.delay.DelaySpec;
import com.mockavior.contract.model.Mode;
import com.mockavior.routing.Router;
import com.mockavior.kafka.model.KafkaScenario;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable runtime representation of compiled contract.
 * This object is NEVER mutated after creation.
 */
public final class ContractSnapshot {

    private final SnapshotVersion version;
    private final Instant createdAt;
    private final Router router;
    private final ContractSnapshot.Settings settings;
    private final Map<String, DelaySpec> routeDelays;
    private final Map<String, KafkaScenario> kafkaScenarios;


    public ContractSnapshot(
            SnapshotVersion version,
            Instant createdAt,
            Router router,
            ContractSnapshot.Settings settings,
            Map<String, DelaySpec> routeDelays,
            Map<String, KafkaScenario> kafkaScenarios
    ) {
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.router = Objects.requireNonNull(router, "router must not be null");
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.routeDelays = routeDelays == null ? Map.of() : Map.copyOf(routeDelays);
        this.kafkaScenarios =
                kafkaScenarios == null ? Map.of() : Map.copyOf(kafkaScenarios);


    }

    public Duration delayForRoute(String routeId) {
        if (routeId == null) {
            return Duration.ZERO;
        }

        DelaySpec spec = routeDelays.get(routeId);
        if (spec == null) {
            return Duration.ZERO;
        }
        return spec.resolve();
    }

    public SnapshotVersion version() {
        return version;
    }

    public Router router() {
        return router;
    }


    public ContractSnapshot.Settings settings() {
        return settings;
    }

    public Map<String, KafkaScenario> kafkaScenarios() {
        return kafkaScenarios;
    }

    @Override
    public String toString() {
        return "ContractSnapshot{" +
                "version=" + version +
                ", createdAt=" + createdAt +
                ", router=" + router +
                '}';
    }

    public static final class Settings {

        private final Mode mode;
        private final int defaultStatus;
        private final URI proxyBaseUri; // nullable

        public Settings(Mode mode, int defaultStatus, URI proxyBaseUri) {
            this.mode = Objects.requireNonNull(mode, "mode must not be null");
            this.defaultStatus = defaultStatus;
            this.proxyBaseUri = proxyBaseUri;
        }

        public Mode mode() {
            return mode;
        }

        public int defaultStatus() {
            return defaultStatus;
        }

        public URI proxyBaseUri() {
            return proxyBaseUri;
        }
    }
}
