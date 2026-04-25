package com.bookie.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bookie.infra.TemplatingEngine.html;

public class ClientChannel {

    private final List<ServerResponse.SseBuilder> streams = new ArrayList<>();
    private final String name;
    private long eventIdCounter = 0;

    private static final Logger logger = LoggerFactory.getLogger(ClientChannel.class);

    public ClientChannel() {
        this("");
    }

    public ClientChannel(String name) {
        this.name = name;
    }

    public synchronized void connect(ServerResponse.SseBuilder builder) {
        logger.debug("Connecting channel {}", name);

        builder.onTimeout(() -> {
            logger.debug("Timeout of channel {}", name);
            removeStream(builder);
        }).onError(e -> {
            logger.debug("Error on channel {}", name, e);
            removeStream(builder);
        }).onComplete(() -> {
            logger.debug("Completed channel {}", name);
            removeStream(builder);
        });

        addStream(builder);
        heartbeat();
    }

    public synchronized boolean isAlive() {
        return !streams.isEmpty();
    }

    public synchronized int getStreamCount() {
        return this.streams.size();
    }

    public synchronized void complete() {
        logger.debug("Completing channel {}", name);
        drainStreams(ServerResponse.SseBuilder::complete);
    }

    public synchronized void fail() {
        drainStreams(stream -> stream.error(new Exception("Connection Aborted by Server")));
    }

    public synchronized void heartbeat() {
        comment("heartbeat");
    }

    public synchronized void comment(String comment) {
        forAllStreams(stream -> stream
                .id(String.valueOf(this.eventIdCounter++))
                .comment(comment)
                .send(), true);
    }

    public synchronized ClientChannel updateURL(String url) {
        return executeScript("history.pushState(null,'','" + url + "')");
    }

    public synchronized ClientChannel executeScript(String script) {
        @SuppressWarnings("BadExpressionStatementJS")
        EscapedHtml scriptFragment = html("""
                <script id="script-runner">${script}</script>
                """, "script", EscapedHtml.rawHtml(script));

        return removeFragment("#script-runner")
                .appendFragment(scriptFragment, "body");
    }

    public synchronized ClientChannel updateFragment(EscapedHtml fragment) {
        return updateFragment(fragment, null, null);
    }

    public synchronized ClientChannel appendFragment(EscapedHtml fragment, String selector) {
        return updateFragment(fragment, selector, "append");
    }

    public synchronized ClientChannel removeFragment(String selector) {
        return updateFragment(EscapedHtml.blank(), selector, "remove");
    }

    public synchronized ClientChannel patchSignals(Map<String, Object> signals) {
        forAllStreams(s -> sendEvent(s, "datastar-patch-signals", "signals " + Util.toJson(signals)));

        return this;
    }

    private ClientChannel updateFragment(EscapedHtml fragment, String selector, String mode) {
        var data = new StringBuilder();
        if (selector != null) {
            data.append("selector ").append(selector).append("\n");
        }

        if (mode != null) {
            data.append("mode ").append(mode).append("\n");
        }

        for (String line : fragment.toString().split("\n")) {
            String trimmed = line.trim();
            if(!trimmed.isBlank()) {
                data.append("elements ").append(trimmed).append("\n");
            }
        }

        forAllStreams(s -> sendEvent(s, "datastar-patch-elements", data.toString()));

        return this;
    }

    private void sendEvent(ServerResponse.SseBuilder s, String eventName, String data) throws IOException {
        s.event(eventName).id(String.valueOf(this.eventIdCounter++));
        s.data(data);
    }

    @FunctionalInterface
    private interface StreamAction {
        void accept(ServerResponse.SseBuilder stream) throws Exception;
    }

    private void forAllStreams(StreamAction action) {
        forAllStreams(action, false);
    }

    private void forAllStreams(StreamAction action, boolean clearOnError) {
        for (ServerResponse.SseBuilder stream : List.copyOf(streams)) {
            try {
                action.accept(stream);
            } catch (Exception e) {
                logger.debug("Exception on channel {}", this.name, e);
                if(clearOnError) {
                    removeStream(stream);
                }
            }
        }
    }

    private void drainStreams(StreamAction action) {
        List<ServerResponse.SseBuilder> snapshot = List.copyOf(streams);
        clearStreams();

        for (ServerResponse.SseBuilder stream : snapshot) {
            try {
                action.accept(stream);
            } catch (Exception ignored) {
            }
        }
    }

    private synchronized void addStream(ServerResponse.SseBuilder builder) {
        streams.add(builder);
    }

    private synchronized void removeStream(ServerResponse.SseBuilder builder) {
        streams.remove(builder);
    }

    private synchronized void clearStreams() {
        streams.clear();
    }

}
