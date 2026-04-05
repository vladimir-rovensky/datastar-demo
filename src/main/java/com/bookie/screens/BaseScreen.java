package com.bookie.screens;

import com.bookie.infra.ClientChannel;
import com.bookie.infra.EscapedHtml;
import com.bookie.infra.SessionRegistry;

import static com.bookie.screens.Shell.shell;

public abstract class BaseScreen {

    private String tabID;
    private final String title;
    private final SessionRegistry sessionRegistry;

    public BaseScreen(String title, SessionRegistry sessionRegistry) {
        this.title = title;
        this.sessionRegistry = sessionRegistry;
    }

    public String getTabID() {
        return tabID;
    }

    public void setTabID(String tabID) {
        this.tabID = tabID;
    }

    public void dispose() {};

    public EscapedHtml render() {
        return shell(this.getTabID())
                .withTitle(title)
                .withToolbar(getToolbarContent())
                .withContent(getContent())
                .render();
    }

    protected abstract EscapedHtml getContent();

    protected EscapedHtml getToolbarContent() {
        return EscapedHtml.html(TradeTicketPopup.getToolbarButtons());
    }

    public void reRender() {
        getUpdateChannel().updateFragment(this.render());
    }

    protected ClientChannel getUpdateChannel() {
        return this.sessionRegistry.getSession(tabID)
                .getClientChannel();
    }
}
