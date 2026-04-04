package com.bookie.infra;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class MessageBus {

    private final Map<Class<?>, List<Consumer<Object>>> subscribers = new HashMap<>();

    public <T> void publish(T event) {
        List<Consumer<Object>> handlers = subscribers.get(event.getClass());
        if (handlers != null) {
            handlers.forEach(h -> h.accept(event));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add((Consumer<Object>) handler);
    }
}
