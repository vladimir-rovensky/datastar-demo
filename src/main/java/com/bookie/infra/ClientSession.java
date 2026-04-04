package com.bookie.infra;

import java.time.Instant;

public class ClientSession {

    private final String tabId;
    private final ClientChannel channel;
    private final long timeoutSeconds;
    private volatile Instant lastActive = Instant.now();

    public ClientSession(String tabId, long timeoutSeconds) {
        this.tabId = tabId;
        this.channel = new ClientChannel(tabId);
        this.timeoutSeconds = timeoutSeconds;
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
}