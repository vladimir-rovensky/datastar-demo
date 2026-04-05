package com.bookie.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientChannel {

    private ServerResponse.SseBuilder sseBuilder;
    private final AtomicBoolean wasConnected = new AtomicBoolean(false);
    private final AtomicBoolean alive = new AtomicBoolean(false);
    private final String tabId;

    private static final Logger logger = LoggerFactory.getLogger(ClientChannel.class);

    public ClientChannel() {
        this("");
    }

    public ClientChannel(String tabId) {
        this.tabId = tabId;
    }

    public synchronized void connect(ServerResponse.SseBuilder builder) {
        this.sseBuilder = builder;
        alive.set(true);
        wasConnected.set(true);

        builder.onTimeout(() -> {
            logger.info("Timeout of channel {}", tabId);
            alive.set(false);
        });

        builder.onError(e -> {
            logger.info("Error on channel {}", tabId, e);
            alive.set(false);
        });

        builder.onComplete(() -> {
            if(!tabId.isEmpty()) {
                logger.info("Completed channel {}", tabId);
            }

            alive.set(false);
        });
    }

    public boolean isAlive() {
        return alive.get();
    }

    public boolean wasConnected() { return wasConnected.get(); }

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

    public synchronized void heartbeat() {
        if (!alive.get() || sseBuilder == null) return;
        try {
             sseBuilder.comment("heartbeat").send();
        } catch (Exception e) {
            alive.set(false);
        }
    }

    public ClientChannel executeScript(String script) {
        return removeFragment("#script-runner")
                .appendFragment(EscapedHtml.html("<script id=\"script-runner\">" + script + "</script>"), "body");
    }

    public ClientChannel updateFragment(EscapedHtml fragment) {
        return updateFragment(fragment, null, null);
    }

    public ClientChannel appendFragment(EscapedHtml fragment, String selector) {
        return updateFragment(fragment, selector, "append");
    }

    public ClientChannel removeFragment(String selector) {
        return updateFragment(EscapedHtml.html(""), selector, "remove");
    }

    public ClientChannel patchSignals(Map<String, Object> signals) {
        if (!alive.get()) return this;
        synchronized (this) {
            try {
                sseBuilder.event("datastar-patch-signals");
                sseBuilder.data("signals " + Util.toJson(signals));
            } catch (Exception e) {
                alive.set(false);
            }
        }
        return this;
    }

    private ClientChannel updateFragment(EscapedHtml fragment, String selector, String mode) {
        if (!alive.get()) return this;

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
                for (String line : fragment.html().split("\n")) {
                    data.append("elements ").append(line).append("\n");
                }

                sseBuilder.data(data.toString());
            } catch (Exception e) {
                alive.set(false);
            }
        }

        return this;
    }
}