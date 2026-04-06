package com.bookie;

import com.bookie.infra.SessionRegistry;
import com.bookie.screens.PositionsScreen;
import com.bookie.screens.TradeTicketPopup;
import com.bookie.screens.TradesScreen;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class Router {

    private final SessionRegistry sessionRegistry;
    private final AutowireCapableBeanFactory beanFactory;

    @Value("${bookie.version}")
    private String appVersion;

    public Router(SessionRegistry sessionRegistry, AutowireCapableBeanFactory beanFactory) {
        this.sessionRegistry = sessionRegistry;
        this.beanFactory = beanFactory;
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .GET("/global-styles.css", this::serveGlobalStyles)
                .GET("/", _ -> ServerResponse.temporaryRedirect(java.net.URI.create("/trades")).build())
                .nest(path(TradesScreen.RoutePrefix), () -> TradesScreen.setupRoutes(sessionRegistry))
                .nest(path(PositionsScreen.RoutePrefix), () -> PositionsScreen.setupRoutes(sessionRegistry))
                .nest(path("/tradeTicket"), () -> beanFactory.createBean(TradeTicketPopup.class).setupRoutes())
                .build();
    }

    private ServerResponse serveGlobalStyles(ServerRequest request) {
        var etag = "\"" + appVersion + "\"";
        if (etag.equals(request.headers().firstHeader(HttpHeaders.IF_NONE_MATCH))) {
            return ServerResponse.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ServerResponse.ok()
                .contentType(MediaType.parseMediaType("text/css"))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate")
                .header(HttpHeaders.ETAG, etag)
                .body(globalStylesResource());
    }

    private Resource globalStylesResource() {
        var fileResource = new FileSystemResource("src/main/resources/static/global-styles.css");
        return fileResource.exists() ? fileResource : new ClassPathResource("/static/global-styles.css");
    }
}
