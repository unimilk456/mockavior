package com.mockavior.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class WhenQueryRepeatedParamsIT extends AbstractMockaviorIT {

    @ParameterizedTest(name = "{index} → {0}")
    @MethodSource("cases")
    void should_match_query_conditions_with_repeated_params(
            String description,
            String query,
            boolean shouldMatch,
            String expectedMatchedId
    ) {
        loadContract("contracts/when-query-tags.yml");

        ResponseEntity<Map<String, Object>> response =
                client.get()
                        .uri("/test" + query)
                        .exchangeToMono(r ->
                                r.toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
                        )
                        .block();

        assertThat(response).as(description).isNotNull();

        if (shouldMatch) {
            assertThat(response.getStatusCode())
                    .as(description)
                    .isEqualTo(HttpStatus.OK);

            assertThat(response.getBody())
                    .isNotNull()
                    .containsEntry("matched", expectedMatchedId);
        } else {
            assertThat(response.getStatusCode())
                    .as(description)
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }


    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(
                        "tag:* matches when tag is present",
                        "?tag=x",
                        true,
                        "tag-present"
                ),
                Arguments.of(
                        "tag:'as' matches when 'as' is present among repeated params",
                        "?tag=as&tag=b",
                        true,
                        "tag-equals"
                ),
                Arguments.of(
                        "tag:any matches when at least one value is present",
                        "?tag=ab",
                        true,
                        "tag-any"
                ),
                Arguments.of(
                        "tag:any does NOT match → fallback to wildcard",
                        "?tag=sc80",
                        true,
                        "tag-present"
                ),
                Arguments.of(
                        "tag:all matches when all required values are present",
                        "?tag=as&tag=ab",
                        true,
                        "tag-all"
                ),
                Arguments.of(
                        "tag:all does NOT match → equals matches",
                        "?tag=as",
                        true,
                        "tag-equals"
                )
        );
    }

    // ============================
    // NEW: any() in isolation
    // ============================

    @Test
    void should_match_any_query_condition_in_isolation() {
        loadContract("contracts/when-query-any-only.yml");

        // any → match
        ResponseEntity<Map<String, Object>> ok =
                client.get()
                        .uri("/test?tag=ab")
                        .exchangeToMono(r ->
                                r.toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
                        )
                        .block();

        assertThat(ok).isNotNull();
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody())
                .isNotNull()
                .containsEntry("matched", "tag-any");

        // none matched → 404
        ResponseEntity<Map<String, Object>> none =
                client.get()
                        .uri("/test?tag=sc80")
                        .exchangeToMono(r ->
                                r.toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
                        )
                        .block();

        assertThat(none).isNotNull();
        assertThat(none.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // param absent → 404
        ResponseEntity<Map<String, Object>> absent =
                client.get()
                        .uri("/test")
                        .exchangeToMono(r ->
                                r.toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
                        )
                        .block();

        assertThat(absent).isNotNull();
        assertThat(absent.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

}
