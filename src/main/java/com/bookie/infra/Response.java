package com.bookie.infra;

import com.bookie.screens.BaseScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

public class Response {

    private static final Logger logger = LoggerFactory.getLogger(Response.class);

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

    public static <T extends BaseScreen> ServerResponse connectUpdates(SessionRegistry sessionRegistry, Class<T> screenType, ServerRequest request) {
        var session = sessionRegistry.getSession(request);
        if (session == null) {
            logger.info("Session lost - possibly server restarted - reloading page.");
            return Response.script("window.location.reload();");
        }

        var screen = session.getScreen(screenType);
        var channel = screen.getChannel();

        var isReconnectAttempt = !request.headers().header("Last-Event-ID").isEmpty();

        return Response.sseCustom(builder -> {
            channel.connect(builder);

            if (isReconnectAttempt) {
                logger.info("Channel lost - broken connection - re-rendering page.");
                screen.reRender();
            }
        });
    }
}
