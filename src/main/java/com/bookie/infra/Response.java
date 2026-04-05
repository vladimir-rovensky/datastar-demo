package com.bookie.infra;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;
import java.util.function.Consumer;

public class Response {

    public static ServerResponse sse(Consumer<ClientChannel> handler) {
        return ServerResponse.sse(b -> {
            var channel = new ClientChannel();
            channel.connect(b);
            handler.accept(channel);
        });
    }

    public static ServerResponse patchSignals(Map<String, Object> signals) {
        return sse(channel -> channel.patchSignals(signals).complete());
    }

    public static ServerResponse html(EscapedHtml content) {
        return ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(content.html());
    }
}
