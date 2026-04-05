package com.bookie;

import com.bookie.infra.ClientSession;
import com.bookie.infra.Response;
import com.bookie.infra.SessionRegistry;
import com.bookie.screens.TradeTicketPopup;
import com.bookie.screens.TradesScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class Router {

    private final SessionRegistry sessionRegistry;
    private final AutowireCapableBeanFactory beanFactory;

    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    public Router(SessionRegistry sessionRegistry, AutowireCapableBeanFactory beanFactory) {
        this.sessionRegistry = sessionRegistry;
        this.beanFactory = beanFactory;
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .POST("/updates", this::handleUpdates)
                .GET("/", _ -> ServerResponse.temporaryRedirect(java.net.URI.create("/trades")).build())
                .nest(path("/trades"), () -> TradesScreen.setupRoutes(sessionRegistry))
                .nest(path("/tradeTicket"), () -> beanFactory.createBean(TradeTicketPopup.class).setupRoutes())
                .build();
    }

    private ServerResponse handleUpdates(ServerRequest request) {
        ClientSession session = sessionRegistry.getSession(request);
        if (session == null) {
            logger.info("Session lost - possibly server restarted - reloading page.");
            return Response.script("window.location.reload();");
        }

        var channel = session.getClientChannel();
        var channelBroken = !channel.isAlive() && channel.wasConnected();

        return Response.sseCustom(b -> {
            channel.connect(b);
            if (channelBroken) {
                logger.info("Channel lost - broken connection or incremental rebuild - re-rendering page.");
                session.reRenderScreen();
            }
        });
    }
}
