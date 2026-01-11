package com.mockavior.it;


import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class EndpointErrorIT extends AbstractMockaviorIT {

    @Test
    void should_return_500_for_specific_endpoint() {
        loadContract("contracts/endpoint-specific-error.yml");

        client.get()
                .uri("/unstable")
                .exchangeToMono(resp -> {
                    assertThat(resp.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    return Mono.empty();
                })
                .block();
    }
}

