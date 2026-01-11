package com.mockavior.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DelayResponseIT extends AbstractMockaviorIT {

    @Test
    void delayed_response_should_wait_and_not_block_other_requests() {

        loadContract("contracts/delay-routing.yml");

        // --- start delayed request (async) ---
        Instant slowStart = Instant.now();

        var slowMono = client.get()
                .uri("/slow")
                .retrieve()
                .toEntity(String.class);

        // --- immediately call fast endpoint ---
        Instant fastStart = Instant.now();

        String fastBody = client.get()
                .uri("/fast")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Instant fastEnd = Instant.now();

        // --- assertions for FAST request ---
        assertThat(fastBody).isEqualTo("FAST");

        Duration fastDuration = Duration.between(fastStart, fastEnd);

        // Fast request must NOT wait for slow one
        assertThat(fastDuration)
                .isLessThan(Duration.ofMillis(300));

        // --- now wait for SLOW response ---
        var slowResponse = slowMono.block();

        Instant slowEnd = Instant.now();

        // --- assertions for SLOW request ---
        assertThat(slowResponse.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(slowResponse.getBody())
                .isEqualTo("SLOW");

        Duration slowDuration =
                Duration.between(slowStart, slowEnd);

        // Delay must be applied (with tolerance)
        assertThat(slowDuration)
                .isGreaterThanOrEqualTo(Duration.ofMillis(1400))
                .isLessThan(Duration.ofMillis(3000));
    }
}
