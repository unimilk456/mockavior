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
        Path workspaceRoot = source.path().toAbsolutePath().getParent();

        if (workspaceRoot == null) {
            log.warn(
                    "Cannot start ContractFileWatcher: contract file has no parent directory: {}",
                    source.path()
            );
            return;
        }

        log.info("Starting contract file watcher for {}", source.path());
        thread = new Thread(() -> run(workspaceRoot), "mockavior-contract-watcher");
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

    private void run(Path workspaceRoot) {
        if (workspaceRoot == null) {
            log.warn("Cannot start to watch: directory is null");
            return;
        }

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

            registerRecursively(workspaceRoot, watchService);
            log.info("Watching contract workspace recursively: {}", workspaceRoot);

            boolean shouldStop = false;

            while (running && !shouldStop) {

                WatchKey key = takeWatchKey(watchService);
                if (key == null) {
                    shouldStop = true;
                    continue;
                }

                boolean reloadRequested = processWatchKey(key, watchService);

                if (!resetWatchKey(key, workspaceRoot)) {
                    shouldStop = true;
                }

                if (reloadRequested && !shouldStop) {
                    reloadService.reload();
                }
            }

        } catch (IOException ex) {
            log.error("Failed to initialize contract workspace watcher", ex);
        } catch (Exception ex) {
            log.error("Unexpected error in contract workspace watcher", ex);
        } finally {
            log.info("Contract workspace watcher stopped");
        }
    }

    private boolean processWatchKey(
            WatchKey key,
            WatchService watchService
    ) {
        Path dir = (Path) key.watchable();
        boolean reloadRequested = false;

        for (WatchEvent<?> event : key.pollEvents()) {
            if (processWatchEvent(event, dir, watchService)) {
                reloadRequested = true;
            }
        }

        return reloadRequested;
    }

    private boolean processWatchEvent(
            WatchEvent<?> event,
            Path dir,
            WatchService watchService
    ) {
        WatchEvent.Kind<?> kind = event.kind();

        if (kind == OVERFLOW) {
            return false;
        }

        Path changed = dir.resolve((Path) event.context());

        log.info("Detected contract workspace change: event={}, path={}", kind.name(), changed);

        if (kind == ENTRY_CREATE) {
            handleDirectoryCreation(changed, watchService);
        }

        return true;
    }

    private boolean resetWatchKey(WatchKey key, Path workspaceRoot) {
        boolean valid = key.reset();
        if (valid) {
            return true;
        }

        Path dir = (Path) key.watchable();

        if (dir.equals(workspaceRoot)) {
            log.warn("WatchKey is no longer valid for workspaceRoot {}, stopping watcher", dir);
            return false;
        }

        log.warn("WatchKey is no longer valid for {}, continuing watcher", dir);
        return true;
    }

    private void registerRecursively(
            Path root,
            WatchService watchService
    ) throws IOException {

        try (var paths = Files.walk(root)) {
            paths.filter(Files::isDirectory)
                    .forEach(dir -> registerDirectory(dir, watchService));
        }
    }

    private void handleDirectoryCreation(Path changed, WatchService watchService) {
        try {
            if (Files.isDirectory(changed)) {
                registerRecursively(changed, watchService);
            }
        } catch (Exception ex) {
            log.warn("Failed to register newly created directory: {}", changed, ex);
        }
    }

    private void registerDirectory(Path dir, WatchService watchService) {
        try {
            dir.register(
                    watchService,
                    ENTRY_CREATE,
                    ENTRY_MODIFY,
                    ENTRY_DELETE
            );
            log.debug("Watching directory: {}", dir);
        } catch (IOException ex) {
            log.warn("Failed to watch directory: {}", dir, ex);
        }
    }

    private WatchKey takeWatchKey(WatchService watchService) {
        try {
            return watchService.take();
        } catch (InterruptedException ex) {
            log.debug("Contract file watcher interrupted");
            Thread.currentThread().interrupt();
            return null;
        }
    }


}
