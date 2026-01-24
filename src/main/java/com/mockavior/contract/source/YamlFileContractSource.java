package com.mockavior.contract.source;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Slf4j
public record YamlFileContractSource(Path path) implements ContractSource {

    public YamlFileContractSource {
        Objects.requireNonNull(path, "path must not be null");
    }

    @Override
    public String load() throws IOException {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);

            log.info(
                    "Contract loaded successfully: path={}, size={} bytes",
                    path.toAbsolutePath(),
                    content.length()
            );

            return content;

        } catch (IOException e) {
            log.error("Failed to load contract from file: {}", path.toAbsolutePath(), e);
            throw e;
        }
    }

    @Override
    public String id() {
        return path.toAbsolutePath().toString();
    }

    @Override
    public void save(String raw) throws IOException {
        Objects.requireNonNull(raw, "raw must not be null");

        log.info("Saving contract to file: {} ({} bytes)", path, raw.length());

        try {
            Path parent = path.getParent();
            Path tempDir = (parent != null) ? parent : Path.of(".");

            if (parent != null) {
                Files.createDirectories(parent);
            }

            // write atomically
            Path tempFile = Files.createTempFile(
                    tempDir,
                    path.getFileName().toString(),
                    ".tmp"
            );

            Files.writeString(tempFile, raw, StandardCharsets.UTF_8);

            Files.move(
                    tempFile,
                    path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

            log.debug("Contract saved successfully to file: {}", path.toAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to save contract to file: {}", path.toAbsolutePath(), e);
            throw e;
        }
    }

    public Path workspaceRoot() {
        return path.getParent();
    }
}
