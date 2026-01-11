package com.mockavior.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class KafkaPollControllerIT extends AbstractMockaviorIT {

    @Test
    void should_peek_messages_non_destructively() {
        loadContract("contracts/kafka-poll.yml");

        client.post()
                .uri(adminPath("/kafka/start/user-events"))
                .retrieve()
                .toBodilessEntity()
                .block();

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() ->
                        assertThat(fetchCount("user.created")).isGreaterThanOrEqualTo(1)
                );

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

        client.post()
                .uri(adminPath("/kafka/start/user-events"))
                .retrieve()
                .toBodilessEntity()
                .block();

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() ->
                        assertThat(fetchCount("user.updated")).isGreaterThanOrEqualTo(1)
                );

        client.post()
                .uri(adminPath("/kafka/poll/{topic}/clear"), "user.updated")
                .retrieve()
                .toBodilessEntity()
                .block();

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() ->
                        assertThat(fetchCount("user.updated")).isZero()
                );
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
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() ->
                        assertThat(fetchCount("user.created")).isGreaterThanOrEqualTo(1)
                );

        Map<String, Object> message =
                client.post()
                        .uri(adminPath("/kafka/poll/{topic}/take"), "user.created")
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        assertThat(message).containsEntry("key", "user-1");

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() ->
                        assertThat(fetchCount("user.created")).isZero()
                );

        ResponseEntity<String> secondTake =
                client.post()
                        .uri(adminPath("/kafka/poll/{topic}/take"), "user.created")
                        .exchangeToMono(resp -> resp.toEntity(String.class))
                        .block();

        assertThat(secondTake.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

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