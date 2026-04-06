package com.bookie.screens;

import com.bookie.infra.ClientChannel;
import com.bookie.infra.EscapedHtml;

import static com.bookie.screens.Shell.shell;

public abstract class BaseScreen {

    private String tabID;
    private ClientChannel channel;
    private final String title;

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

    public void reRender() {
        channel.updateFragment(this.render());
    }

}
