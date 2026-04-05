package com.bookie.infra;

import com.bookie.screens.BaseScreen;

import java.time.Instant;

public class ClientSession {

    private final String tabId;
    private final ClientChannel channel;
    private final BaseScreen screen;
    private final long timeoutSeconds;
    private volatile Instant lastActive = Instant.now();

    public ClientSession(String tabId, BaseScreen screen, long timeoutSeconds) {
        this.tabId = tabId;
        this.screen = screen;
        this.channel = new ClientChannel(tabId);
        this.timeoutSeconds = timeoutSeconds;
        this.touch();
    }

    public ClientChannel getClientChannel() {
        return channel;
    }

    public void touch() {
        lastActive = Instant.now();
    }

    public boolean isAbandoned() {
        return !channel.isAlive() && lastActive.isBefore(Instant.now().minusSeconds(timeoutSeconds));
    }

    public String getTabId() {
        return tabId;
    }

    public <T> T getScreen(Class<T> clazz) {
        return clazz.cast(screen);
    }

    public void reRenderScreen() {
        this.screen.reRender();
    }

    public ClientChannel reloadStylesheet() {
        return this.getClientChannel().executeScript(
                "document.querySelectorAll('link[rel=\"stylesheet\"]').forEach(l => l.href = l.href.split('?')[0] + '?' + Date.now())");
    }
}