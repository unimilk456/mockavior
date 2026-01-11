package com.mockavior.it;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFallbackIT extends AbstractMockaviorIT {

    @Test
    void should_return_404_for_unknown_endpoint() {
        loadContract("contracts/strict-default-404.yml");

        Integer statusCode = client.get()
                .uri("/non-existent")
                .exchangeToMono(response -> response.toBodilessEntity()
                        .map(entity -> entity.getStatusCode().value()))
                .block();

        assertThat(statusCode).isEqualTo(404);
    }
}
