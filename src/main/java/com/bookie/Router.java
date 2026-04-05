package com.bookie;

import com.bookie.infra.ClientSession;
import com.bookie.infra.SessionRegistry;
import com.bookie.screens.TradeTicketPopup;
import com.bookie.screens.TradesScreen;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;

import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class Router {

    private final SessionRegistry sessionRegistry;
    private final AutowireCapableBeanFactory beanFactory;

    public Router(SessionRegistry sessionRegistry, AutowireCapableBeanFactory beanFactory) {
        this.sessionRegistry = sessionRegistry;
        this.beanFactory = beanFactory;
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .POST("/updates", this::handleUpdates)
                .nest(path("/trades"), () -> TradesScreen.setupRoutes(sessionRegistry))
                .nest(path("/tradeTicket"), () -> beanFactory.createBean(TradeTicketPopup.class).setupRoutes())
                .build();
    }

    private ServerResponse handleUpdates(ServerRequest request) {
        ClientSession session = sessionRegistry.getSession(request);
        if(session == null) {
            return ServerResponse.ok()
                    .contentType(new MediaType("text", "JavaScript"))
                    .body("window.location.reload();");
        }

        var channel = session.getClientChannel();
        return ServerResponse.sse(channel::connect, Duration.ZERO);
    }
}
