package com.mockavior.kafka.compiler;

import com.mockavior.contract.payload.BodyResolver;
import com.mockavior.contract.payload.ResolvedBody;
import com.mockavior.kafka.model.KafkaMessage;
import com.mockavior.kafka.model.KafkaRecord;
import com.mockavior.kafka.model.KafkaScenario;
import com.mockavior.kafka.raw.RawKafkaMessage;
import com.mockavior.kafka.raw.RawKafkaScenario;
import com.mockavior.kafka.raw.RawKafkaSection;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compiles raw Kafka scenarios from contract into runtime models.
 * Stateless and thread-safe.
 */
@Slf4j
public final class KafkaScenarioCompiler {

    private final BodyResolver bodyResolver;

    public KafkaScenarioCompiler(BodyResolver bodyResolver) {
        this.bodyResolver = Objects.requireNonNull(bodyResolver);
    }

    public KafkaScenario compile(RawKafkaScenario raw) {
        Objects.requireNonNull(raw, "raw kafkaScenario must not be null");

        String scenarioId = raw.getId();
        int scenarioRepeat = raw.getRepeat() != null ? raw.getRepeat() : 1;

        log.debug("Compiling Kafka scenario: id={}, repeat={}, messages={}", scenarioId, scenarioRepeat, raw.getMessages().size());

        List<KafkaRecord> records =
                raw.getMessages().stream()
                        .map(this::compileMessage)
                        .toList();

        return new KafkaScenario(
                scenarioId,
                scenarioRepeat,
                records
        );
    }

    public Map<String, KafkaScenario> compileAll(RawKafkaSection section) {
        Objects.requireNonNull(section, "raw kafka section must not be null");

        Map<String, KafkaScenario> result = new HashMap<>();

        for (RawKafkaScenario rawScenario : section.scenarios()) {
            KafkaScenario scenario = compile(rawScenario);

            if (result.containsKey(scenario.id())) {
                throw new IllegalArgumentException(
                        "Duplicate Kafka scenario id: " + scenario.id()
                );
            }

            result.put(scenario.id(), scenario);

        }

        log.info("Kafka scenarios compiled successfully: count={}", result.size());

        return Map.copyOf(result);
    }

    private KafkaRecord compileMessage(RawKafkaMessage raw) {

        if (raw.topic == null || raw.topic.isBlank()) {
            throw new IllegalArgumentException("kafkaScenario.messages[].topic must not be empty");
        }

        int repeat = raw.repeat != null ? raw.repeat : 1;

        ResolvedBody resolvedValue =
                bodyResolver.resolve(
                        raw.getValue(),
                        raw.getValueFile()
                );

        KafkaMessage message = new KafkaMessage(
                raw.getTopic(),
                raw.getKey(),
                resolvedValue,
                repeat,
                raw.delay
        );

        return new KafkaRecord(raw.topic, message);
    }
}
