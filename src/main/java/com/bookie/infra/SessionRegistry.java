package com.bookie.infra;

import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final long sessionTimeoutSeconds;

    private static final Logger logger = LoggerFactory.getLogger(SessionRegistry.class);

    public SessionRegistry(@Value("${bookie.session.timeout-seconds}") long sessionTimeoutSeconds) {
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    }

    public synchronized ClientSession getOrCreate(String tabId) {
        var session = sessions.computeIfAbsent(tabId, _ -> new ClientSession(tabId, sessionTimeoutSeconds));
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
                logger.info("Cleaning session {}", entry.getValue().getTabId());
            }
            return abandoned;
        });
    }
}