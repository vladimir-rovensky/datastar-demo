package com.bookie.infra;

import com.bookie.screens.TradesScreen;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class Router {

    private final TradesScreen tradesScreen;

    public Router(TradesScreen tradesScreen) {
        this.tradesScreen = tradesScreen;
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .add(tradesScreen.tradesRoutes())
                .build();
    }
}