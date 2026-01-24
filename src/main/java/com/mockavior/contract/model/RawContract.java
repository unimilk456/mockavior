package com.mockavior.contract.model;

import com.mockavior.kafka.raw.RawKafkaSection;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public final class RawContract {

    private final int version;
    private final Settings settings;
    private final Meta meta;
    private final List<RawEndpoint> endpoints;
    private final RawKafkaSection kafka;


    public RawContract(
            int version,
            Meta meta,
            Settings settings,
            List<RawEndpoint> endpoints,
            RawKafkaSection kafka

    ) {
        this.version = version;
        this.meta = meta;
        this.settings = settings;
        this.endpoints = List.copyOf(
                Objects.requireNonNull(endpoints, "endpoints must not be null")
        );
        this.kafka = kafka;
    }

    public int version() {
        return version;
    }

    public RawKafkaSection kafka() {
        return kafka;
    }

    public Settings settings() {
        return settings;
    }

    // ------------------------------------------------------------------
    // Factory from YAML map (MVP)
    // ------------------------------------------------------------------

    public static RawContract fromMap(Map<String, Object> data) {
        Objects.requireNonNull(data, "data must not be null");

        try {
            int version = ((Number) data.getOrDefault("version", 1)).intValue();

            List<RawEndpoint> endpoints = Collections.emptyList();

            Object endpointsObj = data.get("endpoints");
            if (endpointsObj != null) {
                if (!(endpointsObj instanceof List<?> list)) {
                    throw new IllegalArgumentException("'endpoints' must be a list");
                }

                endpoints =
                        list.stream()
                                .map(e -> RawEndpoint.fromMap((Map<String, Object>) e))
                                .toList();
            }

            // ---------- settings (optional) ----------
            Settings settings = null;
            Object settingsObj = data.get("settings");
            if (settingsObj instanceof Map<?, ?>) {
                settings = Settings.fromMap((Map<String, Object>) settingsObj);
            }

            // ---------- meta (optional) ----------
            Meta meta = null;
            Object metaObj = data.get("meta");
            if (metaObj instanceof Map<?, ?>) {
                meta = Meta.fromMap((Map<String, Object>) metaObj);
            }

            // ---------- kafka (optional) ----------
            RawKafkaSection kafka = null;
            Object kafkaObj = data.get("kafka");
            if (kafkaObj instanceof Map<?, ?>) {
                kafka = RawKafkaSection.fromMap((Map<String, Object>) kafkaObj);
            }

            // ---------- validation ----------
            if (endpoints.isEmpty() && kafka == null) {
                throw new IllegalArgumentException(
                        "Contract must contain at least one of: 'endpoints' or 'kafka'"
                );
            }

            log.info(
                    "Raw contract parsed: version={}, endpoints={}",
                    version,
                    endpoints.size()
            );

            if (log.isDebugEnabled()) {
                endpoints.forEach(e ->
                        log.debug(
                                "Endpoint parsed: id={}, method={}, path={}",
                                e.id(),
                                e.request().method(),
                                e.request().path()
                        )
                );
            }

            return new RawContract(
                    version,
                    meta,
                    settings,
                    endpoints,
                    kafka
            );
        }
        catch (Exception e) {
            log.error("Failed to parse raw contract", e);
            throw e;
        }
    }

    public List<RawEndpoint> endpoints() {
        return endpoints;
    }
}
