package com.bookie.infra;

import com.bookie.screens.BaseScreen;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ClientSession {

    private final String tabId;
    private final Map<Class<? extends BaseScreen>, BaseScreen> screens = new HashMap<>();
    private final long timeoutSeconds;
    private volatile Instant lastActive = Instant.now();

    public ClientSession(String tabId, long timeoutSeconds) {
        this.tabId = tabId;
        this.timeoutSeconds = timeoutSeconds;
        this.touch();
    }

    public void touch() {
        lastActive = Instant.now();
    }

    public boolean isAbandoned() {
        var anyChannelAlive = screens.values().stream().anyMatch(screen -> screen.getChannel().isAlive());
        return !anyChannelAlive && lastActive.isBefore(Instant.now().minusSeconds(timeoutSeconds));
    }

    public String getTabId() {
        return tabId;
    }

    public void addScreen(BaseScreen screen) {
        screens.put(screen.getClass(), screen);
    }

    public <T> T getScreen(Class<T> clazz) {
        return clazz.cast(screens.get(clazz));
    }

    public void reRenderScreen() {
        screens.values().forEach(BaseScreen::reRender);
    }

    public void dispose() {
        screens.values().forEach(BaseScreen::dispose);
    }

    public void heartbeatAllChannels() {
        screens.values().forEach(screen -> screen.getChannel().heartbeat());
    }

    public void failAllChannels() {
        screens.values().forEach(screen -> screen.getChannel().fail());
    }

    public void reloadAllStylesheets() {
        var script = "document.querySelectorAll('link[rel=\"stylesheet\"]').forEach(l => l.href = l.href.split('?')[0] + '?' + Date.now())";
        screens.values().forEach(screen -> screen.getChannel().executeScript(script));
    }
}
