package com.mockavior.contract.payload;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public record InlineBodySource(
        Object body,
        ObjectMapper objectMapper
) implements BodySource {

    @Override
    public ResolvedBody resolve() {
        try {
            if (body == null) {
                log.debug("Inline body is null, resolving to empty payload");
                return new ResolvedBody(new byte[0], BodySourceType.INLINE);
            }

            if (body instanceof String string) {
                log.debug("Resolving inline body from String");
                return new ResolvedBody(string.getBytes(StandardCharsets.UTF_8),
                        BodySourceType.INLINE);
            }

            log.debug("Resolving inline body via ObjectMapper (type={})",
                    body.getClass().getName());

            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return new ResolvedBody(bytes, BodySourceType.INLINE);

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to resolve inline body", ex);
        }
    }
}
