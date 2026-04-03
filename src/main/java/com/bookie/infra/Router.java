package com.bookie.infra;

import com.bookie.screens.TradesScreen;
import jakarta.servlet.ServletException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Map;

@Configuration
public class Router {

    private final TradesScreen tradesScreen;
    private final SessionRegistry sessionRegistry;

    public Router(TradesScreen tradesScreen, SessionRegistry sessionRegistry) {
        this.tradesScreen = tradesScreen;
        this.sessionRegistry = sessionRegistry;
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .POST("/updates", this::handleUpdates)
                .add(tradesScreen.tradesRoutes())
                .build();
    }

    private ServerResponse handleUpdates(ServerRequest request) throws ServletException, IOException {
        var tabId = (String)request.body(Map.class).get("tabId");
        var channel = sessionRegistry.getOrCreate(tabId).getClientChannel();
        return ServerResponse.sse(channel::connect, Duration.ofDays(365));
    }
}