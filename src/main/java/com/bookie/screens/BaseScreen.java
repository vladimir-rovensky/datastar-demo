package com.bookie.screens;

import com.bookie.infra.ClientChannel;
import com.bookie.infra.EscapedHtml;
import com.bookie.infra.RouteInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.bookie.screens.Shell.shell;

public abstract class BaseScreen {

    @Value("${bookie.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${bookie.open-when-hidden:false}")
    private boolean openWhenHidden;

    private AtomicReference<RouteInfo> routeInfo;
    private ClientChannel channel;
    private final String title;
    private long stateVersion = 0;
    private long renderedStateVersion = 0;

    public BaseScreen(String title) {
        this.title = title;
    }

    public RouteInfo getRouteInfo() {
        return routeInfo.get();
    }

    public void setRouteInfo(AtomicReference<RouteInfo> routeInfo) {
        this.routeInfo = routeInfo;
        this.channel = new ClientChannel(title + " - " + routeInfo.get().tabId());
    }

    protected void updateRouteInfo(RouteInfo newInfo) {
        routeInfo.set(newInfo);
    }

    public ClientChannel getChannel() {
        return channel;
    }

    public void dispose() {}

    public EscapedHtml render() {
        var info = routeInfo.get();
        return shell(info)
                .withTitle(title)
                .withUpdateURL(getUpdateURL())
                .withOpenWhenHidden(openWhenHidden)
                .withToolbar(getToolbarContent())
                .withContent(getContent())
                .render();
    }

    protected abstract EscapedHtml getContent();

    protected EscapedHtml getToolbarContent() {
        return TradeTicketPopup.getToolbarButtons();
    }

    protected String getUpdateURL() {
        return null;
    }

    protected ServerResponse handleInitialRender(ServerRequest request, Supplier<EscapedHtml> render) {
        this.renderedStateVersion = this.stateVersion;

        if (!cacheEnabled) {
            return ServerResponse.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(render.get().toString());
        }

        var currentETag = "\"" + this.getETag() + "\"";
        var ifNoneMatch = request.headers().firstHeader("If-None-Match");
        if (currentETag.equals(ifNoneMatch)) {
            return ServerResponse.status(304).build();
        }

        String body = render.get().toString();

        return ServerResponse.ok()
                .header(HttpHeaders.ETAG, currentETag)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate")
                .contentType(MediaType.TEXT_HTML)
                .body(body);
    }

    protected String getETag() {
        return routeInfo.get().tabId().localID() + "-" + this.stateVersion;
    }

    public boolean hasPendingUpdate() {
        return stateVersion > renderedStateVersion;
    }

    public void triggerUpdate() {
        this.stateVersion++;
        this.reRender();
    }

    public void reRender() {
        if(this.channel != null) {
            channel.updateFragment(this.getContent());
        }
    }

}
