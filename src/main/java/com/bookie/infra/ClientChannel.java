package com.bookie.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.concurrent.atomic.AtomicBoolean;

public class ClientChannel {

    private ServerResponse.SseBuilder sseBuilder;
    private final AtomicBoolean alive = new AtomicBoolean(false);
    private final String tabId;

    private static final Logger logger = LoggerFactory.getLogger(ClientChannel.class);

    public ClientChannel(String tabId) {
        this.tabId = tabId;
    }

    public synchronized void connect(ServerResponse.SseBuilder builder) {
        this.sseBuilder = builder;
        alive.set(true);

        builder.onTimeout(() -> {
            logger.info("Timeout of channel {}", tabId);
            alive.set(false);
        });

        builder.onError(e -> {
            logger.info("Error on channel {}", tabId, e);
            alive.set(false);
        });

        builder.onComplete(() -> {
            logger.info("Completed channel {}", tabId);
            alive.set(false);
        });
    }

    public boolean isAlive() {
        return alive.get();
    }

    public synchronized void complete() {
        if (alive.getAndSet(false) && sseBuilder != null) {
            sseBuilder.complete();
        }
    }

    public synchronized void fail() {
        alive.set(false);
        if (sseBuilder != null) {
            sseBuilder.error(new Exception("Connection Aborted by Server"));
        }
    }

    public void updateFragment(String fragment) {
        updateFragment(fragment, null, null);
    }

    public void updateFragment(String fragment, String selector, String mode) {
        if (!alive.get()) return;

        synchronized (this) {
            try {
                sseBuilder.event("datastar-patch-elements");

                var data = new StringBuilder();
                if (selector != null) {
                    data.append("selector ").append(selector).append("\n");
                }
                if (mode != null) {
                    data.append("mode ").append(mode).append("\n");
                }
                for (String line : fragment.split("\n")) {
                    data.append("elements ").append(line).append("\n");
                }

                sseBuilder.data(data.toString());
            } catch (Exception e) {
                alive.set(false);
            }
        }
    }
}