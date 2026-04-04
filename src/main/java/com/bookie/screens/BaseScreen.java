package com.bookie.screens;

import com.bookie.infra.ClientChannel;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.function.Consumer;

public class BaseScreen {

    protected String tabID;

    protected static ServerResponse html(String content) {
        return ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(content);
    }

    protected static ServerResponse removeFragment(String selector) {
        return sse(channel -> channel.removeFragment(selector).complete());
    }

    protected static ServerResponse updateFragment(String fragment, String selector, String mode) {
        return sse(channel -> channel.updateFragment(fragment, selector, mode).complete());
    }

    protected static ServerResponse sse(Consumer<ClientChannel> handler) {
        return ServerResponse.sse(b -> {
            var channel = new ClientChannel();
            channel.connect(b);
            handler.accept(channel);
        });
    }

    public String getTabID() {
        return tabID;
    }

    public void setTabID(String tabID) {
        this.tabID = tabID;
    }
}
