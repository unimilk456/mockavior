package com.mockavior.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class KafkaPollControllerIT extends AbstractMockaviorIT {

    private static final Duration AWAIT_MAX = Duration.ofSeconds(3);

    private static final Duration FIXED_DELAY = Duration.ofMillis(1000);

    private static final Duration RANDOM_MIN_DELAY = Duration.ofMillis(300);

    private static final Duration RANDOM_MAX_DELAY = Duration.ofMillis(800);

    private static final Duration TOLERANCE = Duration.ofMillis(300);

    private static final Duration COMBINED_MIN_DELAY = FIXED_DELAY.plus(RANDOM_MIN_DELAY);

    private static final Duration COMBINED_MAX_DELAY = FIXED_DELAY.plus(RANDOM_MAX_DELAY).plus(TOLERANCE);


    @Test
    void should_peek_messages_non_destructively() {
        loadContract("contracts/kafka-poll.yml");

        clearTopic("user.created");

        client.post()
                .uri(adminPath("/kafka/start/user-events"))
                .retrieve()
                .toBodilessEntity()
                .block();

        await()
                .atMost(AWAIT_MAX)
                .untilAsserted(() -> {
                    Map<String, Object> response =
                            client.get()
                                    .uri(adminPath("/kafka/poll/{topic}"), "user.created")
                                    .retrieve()
                                    .bodyToMono(Map.class)
                                    .block();

                    assertThat(response.get("count")).isEqualTo(1);
                });

        Map<String, Object> response1 =
                client.get()
                        .uri(adminPath("/kafka/poll/{topic}"), "user.created")
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        assertThat(response1).containsEntry("count", 1);

        Map<String, Object> response2 =
                client.get()
                        .uri(adminPath("/kafka/poll/{topic}"), "user.created")
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        assertThat(response2).containsEntry("count", 1);
    }

    @Test
    void should_return_no_content_when_taking_from_empty_topic() {
        loadContract("contracts/kafka-poll.yml");

        clearTopic("user.created");

        HttpStatusCode status =
                client.post()
                        .uri(adminPath("/kafka/poll/{topic}/take"), "user.created")
                        .exchangeToMono(r -> r.toBodilessEntity().map(ResponseEntity::getStatusCode))
                        .block();

        assertThat(status.value()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void should_clear_topic() {
        loadContract("contracts/kafka-poll.yml");

        clearTopic("user.updated");

        client.post()
                .uri(adminPath("/kafka/start/user-events"))
                .retrieve()
                .toBodilessEntity()
                .block();

        await()
                .atMost(AWAIT_MAX)
                .untilAsserted(() ->
                        assertThat(fetchCount("user.created"))
                                .isGreaterThanOrEqualTo(1)
                );


        clearTopic("user.updated");

        assertThat(fetchCount("user.updated")).isZero();
    }

    @Test
    void should_take_message_and_remove_it() {
        loadContract("contracts/kafka-poll.yml");

        clearTopic("user.created");
        clearTopic("user.updated");

        client.post()
                .uri(adminPath("/kafka/start/user-events"))
                .retrieve()
                .toBodilessEntity()
                .block();

        await()
                .atMost(AWAIT_MAX)
                .untilAsserted(() ->
                        assertThat(fetchCount("user.created")).isEqualTo(1)
                );

        // берём одно сообщение
        Map<String, Object> message =
                client.post()
                        .uri(adminPath("/kafka/poll/{topic}/take"), "user.created")
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        assertThat(message).containsEntry("key", "user-1");

        ResponseEntity<String> secondTake =
                client.post()
                        .uri(adminPath("/kafka/poll/{topic}/take"), "user.created")
                        .exchangeToMono(resp -> resp.toEntity(String.class))
                        .block();

        assertThat(secondTake.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }


    @Test
    void kafka_fixed_delay_should_delay_message_emission() {
        loadContract("contracts/kafka-delay-fixed.yml");

        clearTopic("delayed.topic");

        Instant start = Instant.now();

        client.post()
                .uri(adminPath("/kafka/start/delayed"))
                .retrieve()
                .toBodilessEntity()
                .block();

        await()
                .atMost(AWAIT_MAX)
                .until(() -> fetchCount("delayed.topic") >= 1);

        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(elapsed)
                .isGreaterThanOrEqualTo(FIXED_DELAY)
                .isLessThan(FIXED_DELAY.plus(TOLERANCE));
    }

    @Test
    void kafka_random_delay_should_be_within_range() {
        loadContract("contracts/kafka-delay-random.yml");

        clearTopic("delayed.topic");

        Instant start = Instant.now();

        client.post()
                .uri(adminPath("/kafka/start/delayed"))
                .retrieve()
                .toBodilessEntity()
                .block();

        await()
                .atMost(AWAIT_MAX)
                .until(() -> fetchCount("delayed.topic") >= 1);

        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(elapsed)
                .isGreaterThanOrEqualTo(RANDOM_MIN_DELAY)
                .isLessThanOrEqualTo(RANDOM_MAX_DELAY.plus(TOLERANCE));
    }

    @Test
    void kafka_fixed_and_random_delay_should_be_combined() {
        loadContract("contracts/kafka-delay-fixed-random.yml");

        clearTopic("delayed.topic");

        Instant start = Instant.now();

        client.post()
                .uri(adminPath("/kafka/start/delayed"))
                .retrieve()
                .toBodilessEntity()
                .block();

        await()
                .atMost(AWAIT_MAX)
                .until(() -> fetchCount("delayed.topic") >= 1);

        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(elapsed)
                .isGreaterThanOrEqualTo(COMBINED_MIN_DELAY)
                .isLessThan(COMBINED_MAX_DELAY);
    }

    /* ============================
       Helpers
       ============================ */

    private void clearTopic(String topic) {
        client.post()
                .uri(adminPath("/kafka/poll/{topic}/clear"), topic)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private int fetchCount(String topic) {
        Map<String, Object> body =
                client.get()
                        .uri(adminPath("/kafka/poll/{topic}"), topic)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        Object count = body.get("count");
        assertThat(count).isInstanceOf(Number.class);
        return ((Number) count).intValue();
    }
}

