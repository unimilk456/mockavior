package com.mockavior.contract.payload;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@AllArgsConstructor
@Slf4j
public final class BodyResolver {

    private final ObjectMapper objectMapper;
    private final Path workspaceRoot;

    public ResolvedBody resolve(Object body, Object bodyFile) {

        BodySource bodySource;

        if (bodyFile != null) {
            log.debug("BodyFile is defined, using FileBodySource");
            bodySource = new FileBodySource(Path.of(bodyFile.toString()), workspaceRoot);
        } else {
            log.debug("Using inline body");
            bodySource = new InlineBodySource(body, objectMapper);
        }

        return bodySource.resolve();
    }
}
