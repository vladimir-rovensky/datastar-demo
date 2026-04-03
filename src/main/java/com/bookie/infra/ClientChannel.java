package com.bookie.infra;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientChannel {

    private SseEmitter emitter;
    private final AtomicBoolean alive = new AtomicBoolean(false);

    public synchronized SseEmitter connect() {
        this.emitter = new SseEmitter(0L);
        this.alive.set(true);
        this.emitter.onCompletion(() -> alive.set(false));
        this.emitter.onError(ex -> alive.set(false));
        return this.emitter;
    }

    public boolean isAlive() {
        return alive.get();
    }

    public void updateFragment(String fragment, String containerSelector) {
        if (!alive.get()) return;

        SseEmitter.SseEventBuilder baseEvent = SseEmitter.event()
                .name("datastar-merge-fragments");

        SseEmitter.SseEventBuilder event = Arrays.stream(fragment.split("\n"))
                .reduce(baseEvent, (e, f) -> e.data("fragments " + f), (_, e2) -> e2)
                .data("selector " + containerSelector);

        this.sendSSE(event);
    }

    private synchronized void sendSSE(SseEmitter.SseEventBuilder builder) {
        try {
            emitter.send(builder.build());
        } catch (Exception e) {
            alive.set(false);
            emitter.completeWithError(e);
        }
    }
}