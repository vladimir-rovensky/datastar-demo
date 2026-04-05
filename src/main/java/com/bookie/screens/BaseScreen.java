package com.bookie.screens;

import com.bookie.infra.ClientChannel;
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

    public String render() {
        return shell(this.getTabID())
                .withTitle(title)
                .withContent(getContent())
                .render();
    }

    protected abstract String getContent();

    protected ClientChannel getUpdateChannel() {
        return this.sessionRegistry.getSession(tabID)
                .getClientChannel();
    }
}
