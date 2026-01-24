package com.mockavior.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockavior.app.admin.service.ContractAdminService;
import com.mockavior.app.admin.service.ContractValidationService;
import com.mockavior.contract.compiler.ContractCompiler;
import com.mockavior.contract.parse.ContractParser;
import com.mockavior.contract.parse.YamlContractParser;
import com.mockavior.contract.source.ContractSource;
import com.mockavior.contract.source.YamlFileContractSource;
import com.mockavior.core.engine.BehaviorEngine;
import com.mockavior.kafka.compiler.KafkaScenarioCompiler;
import com.mockavior.reload.ReloadService;
import com.mockavior.reload.watch.ContractFileWatcher;
import com.mockavior.runtime.RequestProcessor;
import com.mockavior.runtime.proxy.HttpProxyClient;
import com.mockavior.runtime.snapshot.SnapshotRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.file.Path;
import java.time.Clock;

@Slf4j
@Configuration
public class RuntimeConfig {

    @Bean
    public BehaviorEngine behaviorEngine() {
        log.info("Initializing BehaviorEngine");
        return new BehaviorEngine();
    }

    @Bean
    public ContractAdminService contractAdminService(
            ContractSource source,
            ContractParser parser,
            ContractCompiler compiler,
            SnapshotRegistry snapshotRegistry
    ) {
        log.info("Initializing ContractAdminService");
        return new ContractAdminService(source, parser, compiler, snapshotRegistry);
    }

    @Bean
    public ContractValidationService contractValidationService(
            ContractParser parser,
            ContractCompiler compiler,
            SnapshotRegistry registry
    ) {
        log.info("Initializing ContractValidationService");
        return new ContractValidationService(parser, compiler, registry);
    }

    /**
     * Path to mockapi.yml (MVP). Replace with @Value later.
     */
    @Bean
    public YamlFileContractSource yamlFileContractSource(
            @Value("${mockavior.contract.path}") String path
    ) {
        log.info("Using contract file: {}", path);
        return new YamlFileContractSource(Path.of(path));
    }

    @Bean
    public HttpProxyClient httpProxyClient() {
        log.info("Initializing HttpProxyClient");
        return new HttpProxyClient();
    }

    @Bean
    public ContractParser contractParser() {
        return new YamlContractParser();
    }

    @Bean
    public ContractCompiler contractCompiler(Clock clock,
                                             KafkaScenarioCompiler kafkaScenarioCompiler,
                                             ObjectMapper objectMapper,
                                             YamlFileContractSource contractSource) {

        Path workspaceRoot = contractSource.workspaceRoot();

        if (workspaceRoot == null) {
            throw new IllegalStateException(
                    "Contract file must be located inside a directory: " + workspaceRoot
            );
        }

        log.info("Initializing ContractCompiler with workspaceRoot={}", workspaceRoot);
        return new ContractCompiler(clock, kafkaScenarioCompiler, objectMapper,  workspaceRoot);
    }

    @Bean
    public SnapshotRegistry snapshotRegistry(
            ContractCompiler compiler,
            ContractParser parser,
            ContractSource source
    ) throws Exception {

        log.info("Loading initial contract snapshot");
        // Initial load must succeed (MVP)
        String rawText = source.load();
        var raw = parser.parse(rawText);

        var compiled = compiler.compile(raw);
        log.info(
                "Initial snapshot loaded: version={}, fallback={}",
                compiled.snapshot().version().value(),
                compiled.fallbackBehavior().getClass().getSimpleName()
        );

        return new SnapshotRegistry(
                compiled.snapshot(),
                compiled.fallbackBehavior()
        );
    }

    @Bean
    public ReloadService reloadService(
            ContractSource source,
            ContractParser parser,
            ContractCompiler compiler,
            SnapshotRegistry registry
    ) {
        log.info("Initializing ReloadService");
        return new ReloadService(source, parser, compiler, registry);
    }

    @Bean
    public RequestProcessor requestProcessor(
            SnapshotRegistry registry,
            BehaviorEngine engine
    ) {
        log.info("Initializing RequestProcessor");
        return new RequestProcessor(registry, engine);
    }

    @Bean
    @Profile("!test")
    public ContractFileWatcher contractFileWatcher(
            YamlFileContractSource source,
            ReloadService reloadService
    ) {
        log.info("Initializing ContractFileWatcher");
        return new ContractFileWatcher(source, reloadService);
    }


}
