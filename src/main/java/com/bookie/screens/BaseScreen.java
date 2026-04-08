package com.bookie.screens;

import com.bookie.infra.ClientChannel;
import com.bookie.infra.EscapedHtml;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.function.Supplier;

import static com.bookie.screens.Shell.shell;

public abstract class BaseScreen {

    private String tabID;
    private ClientChannel channel;
    private final String title;
    private long stateVersion = 0;

    public BaseScreen(String title) {
        this.title = title;
    }

    public String getTabID() {
        return tabID;
    }

    public void setTabID(String tabID) {
        this.tabID = tabID;
        this.channel = new ClientChannel(title + " - " + tabID);
    }

    public ClientChannel getChannel() {
        return channel;
    }

    public void dispose() {}

    public EscapedHtml render() {
        return shell(this.getTabID())
                .withTitle(title)
                .withUpdateURL(getUpdateURL())
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
        var currentETag = this.getETag();
        var ifNoneMatch = request.headers().firstHeader("If-None-Match");
        if (currentETag.equals(ifNoneMatch)) {
            return ServerResponse.status(304).build();
        }

        String body = render.get().toString();

        return ServerResponse.ok()
                .header(HttpHeaders.ETAG, "\"" + currentETag + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate")
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(body);
    }

    protected String getETag() {
        return this.tabID + "-" + this.stateVersion;
    }

    public void triggerUpdate() {
        this.stateVersion++;
        this.reRender();;
    }

    public void reRender() {
        channel.updateFragment(this.render());
    }

}
