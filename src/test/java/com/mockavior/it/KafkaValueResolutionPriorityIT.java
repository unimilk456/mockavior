package com.mockavior.it;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaValueResolutionPriorityIT extends AbstractMockaviorIT {

    private static final String TEST_TOPIC = "test.topic";

    @ParameterizedTest(name = "{index} → contract={0}, scenario={1}, expectedSource={2}")
    @MethodSource("valueResolutionCases")
    void should_resolve_kafka_value_source_correctly(
            String contractPath,
            String scenarioId,
            String expectedSource
    ) {
        loadContract(contractPath);

        clearTopic();

        startScenario(scenarioId);

        Map<String, Object> message = takeMessage();

        Map<String, Object> decoded = decodeKafkaValue(message);

        assertThat(decoded)
                .containsEntry("source", expectedSource);
    }

    static Stream<Arguments> valueResolutionCases() {
        return Stream.of(
                Arguments.of(
                        "contracts/kafka-value-inline.yml",
                        "inline",
                        "inline"
                ),
                Arguments.of(
                        "contracts/kafka-value-file.yml",
                        "file",
                        "file"
                ),
                Arguments.of(
                        "contracts/kafka-value-both.yml",
                        "both",
                        "file"
                )
        );
    }

    /* ============================
       Helpers
       ============================ */

    private void startScenario(String scenarioId) {
        client.post()
                .uri(adminPath("/kafka/start/{id}"), scenarioId)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private Map<String, Object> takeMessage() {
        return client.post()
                .uri(adminPath("/kafka/poll/{topic}/take"), TEST_TOPIC)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeKafkaValue(Map<String, Object> message) {
        Object value = message.get("value");

        assertThat(value)
                .as("Kafka message value must be a map")
                .isInstanceOf(Map.class);

        return (Map<String, Object>) value;
    }

    private void clearTopic() {
        client.post()
                .uri(adminPath("/kafka/poll/{topic}/clear"), TEST_TOPIC)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
