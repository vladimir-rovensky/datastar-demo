package com.bookie.infra;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Component
public class EventBus {

    private final Map<Class<?>, List<Consumer<Object>>> subscribers = new HashMap<>();
    private final ExecutorService eventProcessor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());

    public synchronized <T> void publish(T event) {
        List<Consumer<Object>> handlers = List.copyOf(subscribers.getOrDefault(event.getClass(), Collections.emptyList()));
        eventProcessor.submit(() -> handlers.forEach(h -> h.accept(event)));
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
