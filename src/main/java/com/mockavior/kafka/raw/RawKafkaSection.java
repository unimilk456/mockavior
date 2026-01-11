package com.mockavior.kafka.raw;

import java.util.List;
import java.util.Map;

public final class RawKafkaSection {

    public final List<RawKafkaScenario> scenarios;


    private RawKafkaSection(List<RawKafkaScenario> scenarios) {
        if (scenarios == null || scenarios.isEmpty()) {
            throw new IllegalArgumentException("kafka.scenarios must not be empty");
        }
        this.scenarios = List.copyOf(scenarios);
    }

    @SuppressWarnings("unchecked")
    public static RawKafkaSection fromMap(Map<String, Object> map) {
        Object scenariosObj = map.get("scenarios");

        if (!(scenariosObj instanceof List<?> list)) {
            throw new IllegalArgumentException("kafka.scenarios must be a list");
        }

        List<RawKafkaScenario> scenarios =
                list.stream()
                        .map(e -> RawKafkaScenario.fromMap((Map<String, Object>) e))
                        .toList();

        return new RawKafkaSection(scenarios);
    }

    public List<RawKafkaScenario> scenarios() {
        return scenarios;
    }
}
