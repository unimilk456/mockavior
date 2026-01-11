package com.mockavior.reload.watch;

import com.mockavior.contract.source.YamlFileContractSource;
import com.mockavior.reload.ReloadService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;

import static java.nio.file.StandardWatchEventKinds.*;

@Slf4j
public final class ContractFileWatcher {

    private final YamlFileContractSource source;
    private final ReloadService reloadService;

    private volatile boolean running = true;
    private Thread thread;

    public ContractFileWatcher(YamlFileContractSource source, ReloadService reloadService) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.reloadService = Objects.requireNonNull(reloadService, "reloadService must not be null");
    }

    @PostConstruct
    public void start() {
        log.info("Starting contract file watcher for {}", source.path());
        thread = new Thread(this::run, "mockavior-contract-watcher");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping contract file watcher");
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void run() {
        Path file = source.path().toAbsolutePath();
        Path dir = file.getParent();
        if (dir == null) {
            log.warn("Cannot start file watcher: parent directory is null for {}", file);
            return;
        }

        log.info("Watching directory {} for changes to {}", dir, file.getFileName());

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

            while (running) {
                WatchKey key;
                try{
                    key = watchService.take();
                }
                catch (InterruptedException e){
                    log.debug("Contract file watcher interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Object ctx = event.context();

                    if (ctx instanceof Path changed) {
                        if (changed.getFileName().toString().equals(file.getFileName().toString())) {

                            log.info(
                                    "Contract file change detected: event={}, file={}",
                                    kind.name(),
                                    changed
                            );

                            log.debug("Triggering contract reload due to {}", kind);

                            // Debounce could be added later; MVP: reload on each change
                            reloadService.reload();
                        }
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    log.warn("WatchKey is no longer valid, stopping watcher");
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Failed to initialize contract file watcher", e);
        } catch (Exception e) {
            log.error("Unexpected error in contract file watcher", e);
        } finally {
            log.info("Contract file watcher stopped");
        }
    }
}
