package com.mockavior.it;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BodyResolutionPriorityIT extends AbstractMockaviorIT {

    @Test
    void should_use_inline_body_when_only_body_is_defined() {
        loadContract("contracts/body-inline-only.yml");

        String body = client.get()
                .uri("/users")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertThat(body).contains("INLINE_BODY");
    }

    @Test
    void should_use_file_body_when_only_bodyFile_is_defined() {
        loadContract("contracts/body-file-only.yml");

        String body = client.get()
                .uri("/users")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertThat(body).contains("FILE_BODY");
    }

    @Test
    void should_prioritize_bodyFile_over_inline_body() {
        loadContract("contracts/body-inline-and-file.yml");

        String body = client.get()
                .uri("/users")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertThat(body)
                .contains("FILE_BODY")
                .doesNotContain("INLINE_BODY");
    }
}
