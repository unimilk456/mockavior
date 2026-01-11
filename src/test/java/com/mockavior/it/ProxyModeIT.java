package com.mockavior.it;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyModeIT extends AbstractMockaviorIT {

    @Test
    void should_proxy_request_to_backend() {
        loadContract("contracts/proxy-passthrough.yml");

        Integer statusCode = client.get()
                .uri("/status/200")
                .exchangeToMono(response -> {
                    return response.statusCode().is2xxSuccessful()
                            ? response.bodyToMono(String.class).thenReturn(200)
                            : response.bodyToMono(String.class).thenReturn(response.statusCode().value());
                })
                .block();

        assertThat(statusCode).isEqualTo(200);
    }

    @Test
    void should_proxy_request_to_httpbin_and_return_teapot() {
        loadContract("contracts/proxy-passthrough.yml");

        ProxyResponse response =
                client.get()
                        .uri("/status/418")
                        .exchangeToMono(r ->
                                r.bodyToMono(String.class)
                                        .map(body -> new ProxyResponse(r.statusCode().value(), body))
                        )
                        .block();

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(418);

        // Key marker that response came from httpbin
        assertThat(response.body()).contains("teapot");
    }

    private record ProxyResponse(
            int statusCode,
            String body
    ) {
    }
}
