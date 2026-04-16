package com.bookie.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Component
public class EventBus {

    private final Map<Class<?>, List<Consumer<Object>>> subscribers = new HashMap<>();
    private final ExecutorService eventProcessor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());

    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);

    public synchronized <T> void publish(T event) {
        List<Consumer<Object>> handlers = List.copyOf(subscribers.getOrDefault(event.getClass(), Collections.emptyList()));
        eventProcessor.submit(() -> handlers.forEach(h -> {
            try {
                h.accept(event);
            } catch(Exception ex) {
                logger.error("Exception in event handler", ex);
            }
         }));
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> Runnable subscribe(Class<T> eventType, Consumer<T> handler) {
        getConsumersFor(eventType)
                .add((Consumer<Object>) handler);

        return () -> unsubscribe(eventType, handler);
    }

    public synchronized <T> boolean unsubscribe(Class<T> eventType, Consumer<T> handler) {
        return getConsumersFor(eventType).remove(handler);
    }


    private <T> List<Consumer<Object>> getConsumersFor(Class<T> eventType) {
        return subscribers.computeIfAbsent(eventType, _ -> new ArrayList<>());
    }
}
