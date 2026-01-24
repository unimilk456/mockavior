package com.mockavior.contract.payload;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public record FileBodySource(
        Path filePath,
        Path workspaceRoot
) implements BodySource {


    @Override
    public ResolvedBody resolve() {
        try {
            Path normalizedWorkspace =
                    workspaceRoot.toAbsolutePath().normalize();

            Path resolvedPath =
                    normalizedWorkspace.resolve(filePath).normalize();

            if (!resolvedPath.startsWith(normalizedWorkspace)) {
                throw new IllegalStateException(
                        "Access outside workspace is forbidden: " + filePath
                );
            }

            if (!Files.exists(resolvedPath)) {
                throw new IllegalStateException(
                        "Body file does not exist: " + resolvedPath
                );
            }

            log.info("Resolving body from file: {}", resolvedPath);

            byte[] bytes = Files.readAllBytes(resolvedPath);
            return new ResolvedBody(bytes);

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to read body file: " + filePath, ex
            );
        }
    }
}
