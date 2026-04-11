package com.bookie.infra;

import com.bookie.screens.BaseScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ClientSession {

    private static final Logger logger = LoggerFactory.getLogger(ClientSession.class);

    private final AtomicReference<RouteInfo> routeInfo;
    private final Map<Class<? extends BaseScreen>, BaseScreen> screens = new HashMap<>();
    private final long timeoutSeconds;
    private Instant lastActive = Instant.now();

    public ClientSession(TabID tabId, long timeoutSeconds) {
        this.routeInfo = new AtomicReference<>(RouteInfo.initial(tabId));
        this.timeoutSeconds = timeoutSeconds;
        this.touch();
    }

    public AtomicReference<RouteInfo> getRouteInfo() {
        return routeInfo;
    }

    public synchronized void touch() {
        lastActive = Instant.now();
    }

    public synchronized boolean isAbandoned() {
        var anyChannelAlive = screens.values().stream().anyMatch(screen -> screen.getChannel().isAlive());
        return !anyChannelAlive && lastActive.isBefore(Instant.now().minusSeconds(timeoutSeconds));
    }

    public TabID getTabId() {
        return routeInfo.get().tabId();
    }

    public synchronized void addScreen(BaseScreen screen) {
        screens.put(screen.getClass(), screen);
    }

    public synchronized <T> T getScreen(Class<T> clazz) {
        return clazz.cast(screens.get(clazz));
    }

    public synchronized void reRenderScreen() {
        screens.values().forEach(BaseScreen::reRender);
    }

    public synchronized void dispose() {
        screens.values().forEach(BaseScreen::dispose);
    }

    public synchronized void heartbeatAllChannels() {
        screens.values().forEach(screen -> screen.getChannel().heartbeat());
    }

    public synchronized void logStatus() {
        screens.forEach((screenClass, screen) -> {
            var streamCount = screen.getChannel().getStreamCount();
            logger.info("  Session {} - {} - {} live streams", getTabId(), screenClass.getSimpleName(), streamCount);
        });
        logger.info("");
    }

    public synchronized int getStreamCount() {
        return screens.values().stream()
                .map(s -> s.getChannel().getStreamCount())
                .reduce(0, Integer::sum);
    }

    public synchronized void failAllChannels() {
        screens.values().forEach(screen -> screen.getChannel().fail());
    }

    public synchronized void reloadAllStylesheets() {
        var script = "document.querySelectorAll('link[rel=\"stylesheet\"]').forEach(l => l.href = l.href.split('?')[0] + '?' + Date.now())";
        screens.values().forEach(screen -> screen.getChannel().executeScript(script));
    }
}
