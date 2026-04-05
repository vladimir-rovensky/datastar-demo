package com.bookie.screens;

import com.bookie.infra.ClientChannel;
import com.bookie.infra.SessionRegistry;

public class BaseScreen {

    private String tabID;
    private final SessionRegistry sessionRegistry;

    public BaseScreen(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public String getTabID() {
        return tabID;
    }

    public void setTabID(String tabID) {
        this.tabID = tabID;
    }

    public void dispose() {};

    protected ClientChannel getUpdateChannel() {
        return this.sessionRegistry.getSession(tabID)
                .getClientChannel();
    }
}
