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

    public synchronized <T extends BaseScreen> ClientSession createSession(Class<T> screenType) {
        var tabID = UUID.randomUUID().toString();

        var screen = beanFactory.createBean(screenType);
        screen.setTabID(tabID);

        var session = new ClientSession(tabID, screen, sessionTimeoutSeconds);
        sessions.put(tabID, session);

        return session;
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
        sessions.values().forEach(s -> s.getClientChannel().executeScript(
                "document.querySelectorAll('link[rel=\"stylesheet\"]').forEach(l => l.href = l.href.split('?')[0] + '?' + Date.now())"));
    }

    @Scheduled(fixedRate = 60, timeUnit = TimeUnit.SECONDS)
    public synchronized void cleanup() {
        sessions.values().forEach(session -> session.getClientChannel().heartbeat());
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
        if(session.getClientChannel() != null) {
            session.getClientChannel().complete();
        }

        if(session.getScreen(BaseScreen.class) != null) {
            session.getScreen(BaseScreen.class).dispose();
        }
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        synchronized (this) {
            sessions.values().forEach(session -> session.getClientChannel().fail());
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
