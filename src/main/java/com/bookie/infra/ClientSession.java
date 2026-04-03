package com.bookie.infra;

import java.time.Instant;

public class ClientSession {

    private final ClientChannel channel = new ClientChannel();
    private volatile Instant lastActive = Instant.now();

    public ClientChannel getClientChannel() {
        return channel;
    }

     public void touch() {
        lastActive = Instant.now();
    }

    public boolean isAbandoned() {
        return !channel.isAlive() && lastActive.isBefore(Instant.now().minusSeconds(1800));
    }
}