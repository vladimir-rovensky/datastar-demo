package com.bookie.infra;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;
import java.util.function.Consumer;

public class Response {
    public static ServerResponse removeFragment(String selector) {
        return sse(channel -> channel.removeFragment(selector).complete());
    }

    public static ServerResponse appendFragment(String fragment, String selector) {
        return updateFragment(fragment, selector, "append");
    }

    public static ServerResponse updateFragment(String fragment) {
        return sse(channel -> channel.updateFragment(fragment).complete());
    }

    public static ServerResponse updateFragment(String fragment, String selector, String mode) {
        return sse(channel -> channel.updateFragment(fragment, selector, mode).complete());
    }

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

    public static ServerResponse html(String content) {
        return ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(content);
    }
}
