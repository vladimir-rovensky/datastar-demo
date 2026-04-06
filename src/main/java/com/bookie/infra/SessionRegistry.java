package com.bookie.infra;

import com.bookie.screens.BaseScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class SessionRegistry implements SmartLifecycle {

    private final HashMap<String, ClientSession> sessions = new HashMap<>();
    private final AutowireCapableBeanFactory beanFactory;
    private final long sessionTimeoutSeconds;
    private volatile boolean running = false;

    private static final Logger logger = LoggerFactory.getLogger(SessionRegistry.class);

    public SessionRegistry(AutowireCapableBeanFactory beanFactory, @Value("${bookie.session.timeout-seconds}") long sessionTimeoutSeconds) {
        this.beanFactory = beanFactory;
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    }

    public synchronized <T> T getScreen(ServerRequest req, Class<T> clazz) {
        return getSession(req).getScreen(clazz);
    }

    public synchronized <T extends BaseScreen> ClientSession getOrCreateSession(Class<T> screenType, ServerRequest request) {
        var tabId = request.param("tabID").orElse(null);
        if (tabId != null) {
            var existing = sessions.get(tabId);
            if (existing != null) {
                existing.touch();
                if (existing.getScreen(screenType) == null) {
                    addScreenToSession(existing, screenType);
                }
                return existing;
            }
        }
        return createSession(screenType);
    }

    public synchronized <T extends BaseScreen> ClientSession createSession(Class<T> screenType) {
        var tabId = UUID.randomUUID().toString();
        var session = new ClientSession(tabId, sessionTimeoutSeconds);
        sessions.put(tabId, session);
        addScreenToSession(session, screenType);
        return session;
    }

    private <T extends BaseScreen> void addScreenToSession(ClientSession session, Class<T> screenType) {
        var screen = beanFactory.createBean(screenType);
        screen.setTabID(session.getTabId());
        session.addScreen(screen);
    }

    public synchronized ClientSession getSession(ServerRequest request) {
        var tabId = request.headers().header("X-tabID").getFirst();
        return getSession(tabId);
    }

    public synchronized ClientSession getSession(String tabId) {
        var session = sessions.get(tabId);
        if (session != null) session.touch();
        return session;
    }

    public synchronized void reRenderAll() {
        sessions.values().forEach(ClientSession::reRenderScreen);
    }

    public synchronized void reloadStylesheets() {
        sessions.values().forEach(ClientSession::reloadAllStylesheets);
    }

    @Scheduled(fixedRate = 60, timeUnit = TimeUnit.SECONDS)
    public synchronized void cleanup() {
        sessions.values().forEach(ClientSession::heartbeatAllChannels);
        sessions.entrySet().removeIf(entry -> {
            var abandoned = entry.getValue().isAbandoned();
            if (abandoned) {
                cleanupSession(entry.getValue());
            }
            return abandoned;
        });
    }

    private static void cleanupSession(ClientSession session) {
        logger.info("Cleaning session {}", session.getTabId());
        session.dispose();
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        synchronized (this) {
            sessions.values().forEach(ClientSession::failAllChannels);
            sessions.clear();
        }
        logger.info("Closed all client channels on shutdown");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
