package com.bookie.infra;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

public class Response {

    public static ServerResponse patchSignals(Map<String, Object> signals) {
        return sse(channel -> channel.patchSignals(signals).complete());
    }

    public static ServerResponse sseCustom(Consumer<ServerResponse.SseBuilder> configure) {
        return ServerResponse.sse(configure, Duration.ZERO);
    }

    public static ServerResponse sse(Consumer<ClientChannel> handler) {
        return ServerResponse.sse(b -> {
            var channel = new ClientChannel();
            channel.connect(b);
            handler.accept(channel);
        }, Duration.ZERO);
    }

    public static ServerResponse html(EscapedHtml content) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content.toString());
    }

    public static ServerResponse script(String script) {
        return ServerResponse.ok()
                .contentType(new MediaType("text", "JavaScript"))
                .body(script);
    }
}
