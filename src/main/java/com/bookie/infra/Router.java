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

import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class Router {

    private final SessionRegistry sessionRegistry;

    public Router(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .POST("/updates", this::handleUpdates)
                .nest(path("/trades"), () -> TradesScreen.setupRoutes(sessionRegistry))
                .build();
    }

    private ServerResponse handleUpdates(ServerRequest request) throws ServletException, IOException {
        var channel = sessionRegistry.getSession(request).getClientChannel();
        return ServerResponse.sse(channel::connect, Duration.ZERO);
    }
}