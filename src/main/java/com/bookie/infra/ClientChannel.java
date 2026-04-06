package com.bookie.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.bookie.infra.TemplatingEngine.html;

public class ClientChannel {

    private ServerResponse.SseBuilder sseBuilder;
    private final AtomicBoolean wasConnected = new AtomicBoolean(false);
    private final AtomicBoolean alive = new AtomicBoolean(false);
    private final String name;

    private static final Logger logger = LoggerFactory.getLogger(ClientChannel.class);

    public ClientChannel() {
        this("");
    }

    public ClientChannel(String name) {
        this.name = name;
    }

    public synchronized void connect(ServerResponse.SseBuilder builder) {
        this.sseBuilder = builder;
        alive.set(true);
        wasConnected.set(true);

        builder.onTimeout(() -> {
            logger.info("Timeout of channel {}", name);
            alive.set(false);
        });

        builder.onError(e -> {
            logger.info("Error on channel {}", name, e);
            alive.set(false);
        });

        builder.onComplete(() -> {
            if(!name.isEmpty()) {
                logger.info("Completed channel {}", name);
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
        EscapedHtml scriptFragment = html("""
                <script id="script-runner">${script}</script>
                """, "script", EscapedHtml.rawHtml(script));

        return removeFragment("#script-runner")
                .appendFragment(scriptFragment, "body");
    }

    public ClientChannel updateFragment(EscapedHtml fragment) {
        return updateFragment(fragment, null, null);
    }

    public ClientChannel appendFragment(EscapedHtml fragment, String selector) {
        return updateFragment(fragment, selector, "append");
    }

    public ClientChannel removeFragment(String selector) {
        return updateFragment(EscapedHtml.blank(), selector, "remove");
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
                for (String line : fragment.toString().split("\n")) {
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