package com.bookie.infra;

import jakarta.servlet.ServletException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class SessionRegistry {

    private final HashMap<String, ClientSession> sessions = new HashMap<>();

    public synchronized ClientSession getOrCreate(String tabId) {
        var session = sessions.computeIfAbsent(tabId, _ -> new ClientSession());
        session.touch();
        return session;
    }

    public synchronized ClientSession get(ServerRequest request) throws ServletException, IOException {
        var body = request.body(Map.class);
        var tabId = (String) body.get("tabId");
        return get(tabId);
    }

    public synchronized ClientSession get(String tabId) {
        var session = sessions.get(tabId);
        if (session != null) session.touch();
        return session;
    }

    @Scheduled(fixedRate = 60, timeUnit = TimeUnit.SECONDS)
    public synchronized void cleanup() {
        sessions.entrySet().removeIf(entry -> {
            var abandoned = entry.getValue().isAbandoned();
            if (abandoned) {
                entry.getValue().getClientChannel().complete();
            }
            return abandoned;
        });
    }
}