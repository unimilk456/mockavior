package com.mockavior.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionalRoutingIT extends AbstractMockaviorIT {

    @Test
    void should_return_response_when_query_condition_matches() {
        loadContract("contracts/conditional-routing.yml");

        String body = client.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/users")
                                .queryParam("active", "true")
                                .build()
                )
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .map(b -> {
                                    assertThat(response.statusCode())
                                            .isEqualTo(HttpStatus.OK);
                                    return b;
                                })
                )
                .block();

        assertThat(body).contains("active users");
    }

    @Test
    void should_return_response_when_header_condition_matches() {
        loadContract("contracts/conditional-routing.yml");

        String body = client.get()
                .uri("/users")
                .header("x-role", "admin")
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .map(b -> {
                                    assertThat(response.statusCode())
                                            .isEqualTo(HttpStatus.OK);
                                    return b;
                                })
                )
                .block();

        assertThat(body).contains("admin users");
    }

    @Test
    void should_fallback_when_conditions_do_not_match() {
        loadContract("contracts/conditional-routing.yml");

        HttpStatusCode status = client.get()
                .uri("/users")
                .exchangeToMono(response ->
                        response.toBodilessEntity()
                                .map(ResponseEntity::getStatusCode)
                )
                .block();

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
