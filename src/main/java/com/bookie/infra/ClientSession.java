package com.bookie.infra;

import jakarta.servlet.http.HttpSession;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientSession {

    private final HttpSession session;
    private final String tabID;

    public static ClientSession createNew(HttpSession rawSession) {
        return new ClientSession(rawSession, UUID.randomUUID().toString());
    }

    public ClientSession(HttpSession session, String tabID) {
        this.session = session;
        this.tabID = tabID;
    }

    public String getTabID() {
        return tabID;
    }

    public ClientChannel getClientChannel() {
        return getOrCreate("updateService", ClientChannel.class, ClientChannel::new);
    }

    public <T> T getOrCreate(String dataID, Class<T> clazz, Supplier<T> getDefaultValue) {
        var attributeID = tabID + " - " +  dataID;
        var value = session.getAttribute(attributeID);
        if(value == null) { {
            session.setAttribute(attributeID, clazz.cast(getDefaultValue.get())); }
        }

        return clazz.cast(session.getAttribute(attributeID));
    }
}
