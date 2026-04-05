package com.bookie.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class HotswapWatcher implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(HotswapWatcher.class);
    private static final Path WATCH_PATH = Path.of("build/classes/java/main");
    private static final Path STATIC_PATH = Path.of("src/main/resources/static");
    private static final long DEBOUNCE_MS = 500;

    private final SessionRegistry sessionRegistry;
    private WatchService watchService;
    private final Map<WatchKey, Path> keyToPath = new HashMap<>();
    private Thread watchThread;
    private ScheduledExecutorService debouncer;
    private ScheduledFuture<?> pending;
    private volatile boolean running = false;

    public HotswapWatcher(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void start() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            debouncer = Executors.newSingleThreadScheduledExecutor();
            Path root = nearestExistingAncestor(WATCH_PATH);
            if (root != null) registerAll(root);
            if (Files.exists(STATIC_PATH)) registerAll(STATIC_PATH);
            watchThread = Thread.ofVirtual().start(this::watch);
            running = true;
        } catch (IOException e) {
            logger.warn("Could not start hotswap watcher: {}", e.getMessage());
        }
    }

    private Path nearestExistingAncestor(Path path) {
        while (path != null && !Files.exists(path)) path = path.getParent();
        return path;
    }

    private void registerAll(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void register(Path dir) throws IOException {
        var key = dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
        keyToPath.put(key, dir);
    }

    private void watch() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }
            if (key == null) continue;

            Path dir = keyToPath.get(key);
            boolean hasClassChange = false;
            boolean hasCssChange = false;

            for (var event : key.pollEvents()) {
                var kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    hasClassChange = true;
                    continue;
                }
                var name = event.context().toString();
                if (kind == StandardWatchEventKinds.ENTRY_CREATE && dir != null) {
                    Path child = dir.resolve(name);
                    if (Files.isDirectory(child)) {
                        try { registerAll(child); } catch (IOException ignored) {}
                    }
                }
                if (name.endsWith(".class")) hasClassChange = true;
                if (name.endsWith(".css")) hasCssChange = true;
            }

            if (!key.reset()) keyToPath.remove(key);
            if (hasClassChange) scheduleReRender();
            if (hasCssChange) scheduleCssReload();
        }
    }

    private synchronized void scheduleReRender() {
        if (pending != null) pending.cancel(false);
        pending = debouncer.schedule(() -> {
            logger.info("Hotswap detected, re-rendering all screens");
            sessionRegistry.reRenderAll();
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    private synchronized void scheduleCssReload() {
        debouncer.schedule(() -> {
            logger.info("CSS change detected, reloading stylesheets");
            sessionRegistry.reloadStylesheets();
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        running = false;
        keyToPath.clear();
        if (watchThread != null) watchThread.interrupt();
        if (debouncer != null) debouncer.shutdownNow();
        if (watchService != null) {
            try { watchService.close(); } catch (IOException ignored) {}
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
