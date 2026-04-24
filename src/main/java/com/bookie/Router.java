package com.bookie;

import com.bookie.components.Notification;
import com.bookie.infra.Response;
import com.bookie.infra.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bookie.infra.UserFacingException;
import com.bookie.screens.PositionsScreen;
import com.bookie.screens.securities.SecuritiesScreen;
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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.EOFException;

import static com.bookie.infra.TemplatingEngine.html;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class Router {

    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    private final SessionRegistry sessionRegistry;
    private final AutowireCapableBeanFactory beanFactory;

    @Value("${bookie.version}")
    private String appVersion;

    @Value("${bookie.cache-enabled:true}")
    private boolean cacheEnabled;

    public Router(SessionRegistry sessionRegistry, AutowireCapableBeanFactory beanFactory) {
        this.sessionRegistry = sessionRegistry;
        this.beanFactory = beanFactory;
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .GET("/global-styles.css", this::serveGlobalStyles)
                .GET("/number-input.js", this::serveNumberInputJs)
                .GET("/", _ -> ServerResponse.temporaryRedirect(java.net.URI.create("/trades")).build())
                .nest(path(TradesScreen.RoutePrefix), () -> TradesScreen.setupRoutes(sessionRegistry))
                .nest(path(PositionsScreen.RoutePrefix), () -> PositionsScreen.setupRoutes(sessionRegistry))
                .nest(path(SecuritiesScreen.RoutePrefix), () -> SecuritiesScreen.setupRoutes(sessionRegistry))
                .nest(path("/tradeTicket"), () -> beanFactory.createBean(TradeTicketPopup.class).setupRoutes())
                .onError(UserFacingException.class, this::handleUserFacingException)
                .onError(Throwable.class, this::handleGenericException)
                .build();
    }

    private ServerResponse serveNumberInputJs(ServerRequest request) {
        if (!cacheEnabled) {
            return ServerResponse.ok()
                    .contentType(MediaType.parseMediaType("application/javascript"))
                    .body(numberInputJsResource());
        }

        var etag = "\"" + appVersion + "\"";
        if (etag.equals(request.headers().firstHeader(HttpHeaders.IF_NONE_MATCH))) {
            return ServerResponse.status(HttpStatus.NOT_MODIFIED).build();
        }

        return ServerResponse.ok()
                .contentType(MediaType.parseMediaType("application/javascript"))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate")
                .header(HttpHeaders.ETAG, etag)
                .body(numberInputJsResource());
    }

    private Resource numberInputJsResource() {
        var fileResource = new FileSystemResource("src/main/resources/static/number-input.js");
        return fileResource.exists() ? fileResource : new ClassPathResource("/static/number-input.js");
    }

    private ServerResponse serveGlobalStyles(ServerRequest request) {
        if (!cacheEnabled) {
            return ServerResponse.ok()
                    .contentType(MediaType.parseMediaType("text/css"))
                    .body(globalStylesResource());
        }

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

    private ServerResponse handleUserFacingException(Throwable ex, ServerRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : ex.toString();
        return Response.html(Notification.notification(html(message))
                .withStyle(Notification.error));
    }

    private ServerResponse handleGenericException(Throwable throwable, ServerRequest request) {
        return switch (throwable) {
            case EOFException ignored -> ServerResponse.ok().build();
            case HttpMessageNotReadableException ignored -> ServerResponse.ok().build();
            case AsyncRequestNotUsableException ignored -> ServerResponse.ok().build();
            default -> {
                logger.error("Unexpected Exception thrown", throwable);
                yield Response.html(Notification.notification(html("Unexpected Server Error - please refresh the page."))
                        .withStyle(Notification.error));
            }
        };
    }
}
