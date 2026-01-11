package com.mockavior.app.admin.service;

import com.mockavior.contract.compiler.ContractCompiler;
import com.mockavior.contract.model.RawContract;
import com.mockavior.contract.parse.ContractParser;
import com.mockavior.contract.source.ContractSource;
import com.mockavior.runtime.snapshot.SnapshotRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Admin service for accessing current contract state.
 * Responsibilities:
 * - provide raw contract text
 * - validate and compile new contracts
 * - manage snapshot lifecycle
 * Does NOT:
 * - parse
 * - validate
 * - reload
 */
@Slf4j
@RequiredArgsConstructor
public final class ContractAdminService {

    @NonNull
    private final ContractSource source;

    @NonNull
    private final ContractParser parser;

    @NonNull
    private final ContractCompiler compiler;

    @NonNull
    private final SnapshotRegistry snapshotRegistry;

    public ContractView getCurrentContract() {
        log.debug("Fetching current contract");

        String raw;
        try {
            // Raw contract is loaded from source; snapshot version refers to active runtime snapshot
            raw = source.load();
        } catch (Exception e) {
            log.error("Failed to load contract source '{}'", source.id(), e);
            throw new IllegalStateException("Contract source unavailable", e);
        }

        String version =
                snapshotRegistry.active()
                        .snapshot()
                        .version()
                        .value();

        log.info(
                "Contract loaded successfully: source={}, snapshotVersion={}",
                source.id(),
                version
        );

        return new ContractView(raw, version);
    }

    public ContractUpdateResult updateContract(
            String expectedVersion,
            String newRawYaml
    ) {

        log.debug("Update contract requested: source={}, expectedVersion={}",source.id(),expectedVersion);

        String currentVersion =
                snapshotRegistry.active()
                        .snapshot()
                        .version()
                        .value();

        if (!Objects.equals(currentVersion, expectedVersion)) {
            log.warn("Optimistic lock failed: expected={}, actual={}",expectedVersion,currentVersion
            );
            throw new OptimisticLockException(currentVersion);
        }

        try {
            log.info("Updating contract {}", source.id());

            RawContract raw = parser.parse(newRawYaml);

            var compiled = compiler.compile(raw);

            source.save(newRawYaml);

            snapshotRegistry.activateNew(
                    compiled.snapshot(),
                    compiled.fallbackBehavior()
            );

            snapshotRegistry.cleanupRetired();

            String newVersion =
                    compiled.snapshot().version().value();

            log.info(
                    "Contract updated successfully: oldVersion={}, newVersion={}",
                    currentVersion,
                    newVersion
            );

            return new ContractUpdateResult(newVersion);

        } catch (OptimisticLockException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update contract", e);
            throw new IllegalStateException("Contract update failed", e);
        }
    }

    public record ContractView(
            String rawYaml,
            String snapshotVersion
    ) {}

    public record ContractUpdateResult(
            String snapshotVersion
    ) {}

    public static final class OptimisticLockException extends RuntimeException {
        private final String actualVersion;

        public OptimisticLockException(String actualVersion) {
            super("Snapshot version mismatch");
            this.actualVersion = actualVersion;
        }

        public String actualVersion() {
            return actualVersion;
        }
    }
}
