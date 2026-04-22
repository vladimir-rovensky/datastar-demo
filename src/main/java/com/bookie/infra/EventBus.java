package com.bookie.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Component
public class EventBus {

    private final Map<Class<?>, List<Consumer<Object>>> subscribers = new HashMap<>();
    private final ExecutorService eventProcessor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

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

    public BatchedSubscriptionBuilder subscribeBatched() {
        return new BatchedSubscriptionBuilder();
    }

    public synchronized <T> boolean unsubscribe(Class<T> eventType, Consumer<T> handler) {
        return getConsumersFor(eventType).remove(handler);
    }

    private <T> List<Consumer<Object>> getConsumersFor(Class<T> eventType) {
        return subscribers.computeIfAbsent(eventType, _ -> new ArrayList<>());
    }

    public class BatchedSubscriptionBuilder {
        private long windowMs = 100;
        private final Map<Class<?>, Consumer<?>> handlers = new LinkedHashMap<>();
        private Runnable batchCallback = () -> {};

        public BatchedSubscriptionBuilder withWindowMs(long windowMs) {
            this.windowMs = windowMs;
            return this;
        }

        public <T> BatchedSubscriptionBuilder on(Class<T> eventType, Consumer<T> handler) {
            handlers.put(eventType, handler);
            return this;
        }

        public BatchedSubscriptionBuilder afterBatchProcessed(Runnable callback) {
            this.batchCallback = callback;
            return this;
        }

        public Runnable subscribe() {
            var batcher = new Batcher(handlers, batchCallback, windowMs);
            var unsubscribers = new ArrayList<Runnable>();

            for (var entry : handlers.entrySet()) {
                unsubscribers.add(subscribeType(entry.getKey(), batcher));
            }

            return () -> {
                unsubscribers.reversed().forEach(Runnable::run);
                batcher.dispose();
            };
        }

        @SuppressWarnings("unchecked")
        private <T> Runnable subscribeType(Class<?> eventType, Batcher batcher) {
            return EventBus.this.subscribe((Class<T>) eventType, event -> batcher.accept(eventType, event));
        }
    }

    private record BufferedEvent(Class<?> type, Object event) {}

    private class Batcher {
        private final Map<Class<?>, Consumer<?>> handlers;
        private final Runnable batchCallback;
        private final long windowMs;
        private final List<BufferedEvent> pendingEvents = new ArrayList<>();
        private boolean windowOpen = false;
        private ScheduledFuture<?> currentWindowTask = null;
        private final Object lock = new Object();

        Batcher(Map<Class<?>, Consumer<?>> handlers, Runnable batchCallback, long windowMs) {
            this.handlers = handlers;
            this.batchCallback = batchCallback;
            this.windowMs = windowMs;
        }

        void accept(Class<?> type, Object event) {
            synchronized (lock) {
                if (windowOpen) {
                    pendingEvents.add(new BufferedEvent(type, event));
                } else {
                    windowOpen = true;
                    currentWindowTask = scheduler.schedule(this::closeWindow, windowMs, TimeUnit.MILLISECONDS);
                    eventProcessor.submit(() -> {
                        invokeHandler(type, event);
                        batchCallback.run();
                    });
                }
            }
        }

        private void closeWindow() {
            eventProcessor.submit(() -> {
                List<BufferedEvent> drained;
                synchronized (lock) {
                    drained = new ArrayList<>(pendingEvents);
                    pendingEvents.clear();
                    windowOpen = false;
                    currentWindowTask = null;
                }
                if (!drained.isEmpty()) {
                    drained.forEach(bufferedEvent -> invokeHandler(bufferedEvent.type(), bufferedEvent.event()));
                    batchCallback.run();
                }
            });
        }

        @SuppressWarnings("unchecked")
        private <T> void invokeHandler(Class<?> type, Object event) {
            try {
                ((Consumer<T>) handlers.get(type)).accept((T) event);
            } catch (Exception ex) {
                logger.error("Exception in batched event handler. Event: {}", event, ex);
            }
        }

        void dispose() {
            synchronized (lock) {
                if (currentWindowTask != null) {
                    currentWindowTask.cancel(false);
                    currentWindowTask = null;
                }
                pendingEvents.clear();
            }
        }
    }
}
