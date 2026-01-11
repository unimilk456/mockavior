package com.mockavior.app.admin.service;

import com.mockavior.contract.compiler.ContractCompiler;
import com.mockavior.contract.model.RawContract;
import com.mockavior.contract.parse.ContractParser;
import com.mockavior.runtime.snapshot.SnapshotRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contract validation service.
 * Responsibilities:
 * - optimistic lock check
 * - parse YAML
 * - compile contract
 * Does NOT:
 * - save
 * - reload
 * - activate snapshot
 */
@Slf4j
@RequiredArgsConstructor
public final class ContractValidationService {

    @NonNull
    private final ContractParser parser;

    @NonNull
    private final ContractCompiler compiler;

    @NonNull
    private final SnapshotRegistry snapshotRegistry;

    public String validate(String ifMatch, String rawYaml) {

        log.debug("Validate contract requested (If-Match={})", ifMatch);

        String currentVersion =
                snapshotRegistry.active()
                        .snapshot()
                        .version()
                        .value();

        // ---------- optimistic locking ----------
        if (ifMatch != null && !currentVersion.equals(ifMatch)) {
            log.warn("Optimistic lock failed during validation: expected={}, actual={}",ifMatch,currentVersion);

            throw new OptimisticLockException(currentVersion);
        }

        try {
            RawContract raw = parser.parse(rawYaml);
            compiler.compile(raw);

            log.debug("Contract validated successfully (baseVersion={})", currentVersion);

            return currentVersion;

        } catch (Exception e) {
            throw new ValidationException(e.getMessage(), e);
        }
    }

    // -----------------------------------------------------

    public static final class OptimisticLockException extends RuntimeException {
        private final String actualVersion;

        public OptimisticLockException(String actualVersion) {

            this.actualVersion = actualVersion;
        }

        public String actualVersion() {
            return actualVersion;
        }
    }

    public static final class ValidationException extends RuntimeException {
        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
