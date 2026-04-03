package com.bookie.infra;

import jakarta.servlet.ServletException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SessionRegistry {

    private final HashMap<String, ClientSession> sessions = new HashMap<>();

    public synchronized ClientSession getOrCreate(String httpSessionId, String tabId) {
        var session = sessions.computeIfAbsent(key(httpSessionId, tabId), _ -> new ClientSession());
        session.touch();
        return session;
    }

    public synchronized  ClientSession get(ServerRequest request) throws ServletException, IOException {
        var body = request.body(Map.class);
        var tabId = (String) body.get("tabId");
        var httpSessionId = request.servletRequest().getSession().getId();
        return get(httpSessionId, tabId);
    }

    public synchronized ClientSession get(String httpSessionId, String tabId) {
        var session = sessions.get(key(httpSessionId, tabId));
        if (session != null) session.touch();
        return session;
    }

    @Scheduled(fixedDelay = 60_000)
    public synchronized void cleanup() {
        sessions.entrySet().removeIf(entry -> {
            var abandoned = entry.getValue().isAbandoned();
            if (abandoned) entry.getValue().getClientChannel().complete();
            return abandoned;
        });
    }

    private String key(String httpSessionId, String tabId) {
        return httpSessionId + ":" + tabId;
    }
}