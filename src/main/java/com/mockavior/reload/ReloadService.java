package com.mockavior.reload;

import com.mockavior.contract.compiler.ContractCompiler;
import com.mockavior.contract.model.CompiledContract;
import com.mockavior.contract.model.RawContract;
import com.mockavior.contract.parse.ContractParser;
import com.mockavior.contract.source.ContractSource;
import com.mockavior.core.snapshot.ContractSnapshot;
import com.mockavior.runtime.snapshot.SnapshotRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public final class ReloadService {

    private final ContractSource source;
    private final ContractParser parser;
    private final ContractCompiler compiler;
    private final SnapshotRegistry snapshotRegistry;

    public ReloadService(
            ContractSource source,
            ContractParser parser,
            ContractCompiler compiler,
            SnapshotRegistry snapshotRegistry
    ) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.compiler = Objects.requireNonNull(compiler, "compiler must not be null");
        this.snapshotRegistry = Objects.requireNonNull(snapshotRegistry, "snapshotRegistry must not be null");
    }

    public ReloadResult reload() {
        log.info("Starting contract reload from source={}", source.id());
        try {
            log.debug("Loading contract source: {}", source.id());
            String rawText = source.load();

            log.debug("Parsing contract ({} bytes)", rawText.length());
            RawContract raw = parser.parse(rawText);

            log.debug("Compiling contract version={}", raw.version());
            CompiledContract compiled = compiler.compile(raw);

            ContractSnapshot snapshot = compiled.snapshot();

            log.info(
                    "Activating new snapshot version={} (source={})",
                    snapshot.version().value(),
                    source.id()
            );

            snapshotRegistry.activateNew(
                    compiled.snapshot(),
                    compiled.fallbackBehavior()
            );

            snapshotRegistry.cleanupRetired();

            log.info(
                    "Contract reload completed successfully: version={}",
                    snapshot.version().value()
            );

            return ReloadResult.success(
                    source.id(),
                    snapshot.version().value()
            );
        } catch (Exception e) {
            log.error(
                    "Contract reload failed for source={}",
                    source.id(),
                    e
            );
            return ReloadResult.failure(source.id(), e);
        }
    }
}
