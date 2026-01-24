package com.mockavior.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DelayResponseIT extends AbstractMockaviorIT {

    private static final Duration FAST_REQUEST_MAX_DURATION =
            Duration.ofMillis(300);

    private static final Duration FIXED_DELAY =
            Duration.ofMillis(1000);

    private static final Duration RANDOM_MIN_DELAY =
            Duration.ofMillis(200);

    private static final Duration RANDOM_MAX_DELAY =
            Duration.ofMillis(600);

    private static final Duration COMBINED_MIN_DELAY =
            FIXED_DELAY.plus(RANDOM_MIN_DELAY);

    private static final Duration COMBINED_MAX_DELAY =
            FIXED_DELAY.plus(RANDOM_MAX_DELAY).plusMillis(300); // tolerance


    @Test
    void fixed_delay_should_be_applied_and_not_block_other_requests() {

        loadContract("contracts/delay-fixed.yml");

        // --- start delayed request ---
        Instant slowStart = Instant.now();

        var slowMono = client.get()
                .uri("/slow")
                .retrieve()
                .toEntity(String.class);

        // --- fast request must not wait ---
        Instant fastStart = Instant.now();

        String fastBody = client.get()
                .uri("/fast")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Instant fastEnd = Instant.now();

        assertThat(fastBody).isEqualTo("FAST");
        assertThat(Duration.between(fastStart, fastEnd))
                .isLessThan(FAST_REQUEST_MAX_DURATION);

        // --- slow response ---
        var slowResponse = slowMono.block();
        Instant slowEnd = Instant.now();

        assertThat(slowResponse.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(slowResponse.getBody())
                .isEqualTo("SLOW");

        Duration slowDuration =
                Duration.between(slowStart, slowEnd);

        assertThat(slowDuration)
                .isGreaterThanOrEqualTo(FIXED_DELAY)
                .isLessThan(FIXED_DELAY.plusMillis(300));
    }


    @Test
    void random_delay_should_be_applied_within_range() {

        loadContract("contracts/delay-random.yml");

        Instant start = Instant.now();

        String body = client.get()
                .uri("/slow")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Instant end = Instant.now();

        assertThat(body).isEqualTo("SLOW");

        Duration duration = Duration.between(start, end);

        assertThat(duration)
                .isGreaterThanOrEqualTo(RANDOM_MIN_DELAY)
                .isLessThanOrEqualTo(RANDOM_MAX_DELAY.plusMillis(300));
    }


    @Test
    void fixed_and_random_delay_should_be_combined() {

        loadContract("contracts/delay-fixed-random.yml");

        Instant start = Instant.now();

        String body = client.get()
                .uri("/slow")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Instant end = Instant.now();

        assertThat(body).isEqualTo("SLOW");

        Duration duration = Duration.between(start, end);

        assertThat(duration)
                .isGreaterThanOrEqualTo(COMBINED_MIN_DELAY)
                .isLessThan(COMBINED_MAX_DELAY);
    }
}
