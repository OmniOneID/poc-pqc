package org.omnione.did.observer.service;

import org.omnione.did.observer.dto.ObservedLogEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LogStreamService {

    private static final int MAX_HISTORY_SIZE = 500;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final List<ObservedLogEvent> history = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));

        for (ObservedLogEvent event : history) {
            sendEvent(emitter, event);
        }
        return emitter;
    }

    public ObservedLogEvent publish(ObservedLogEvent event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(OffsetDateTime.now());
        }

        history.add(event);
        trimHistory();

        for (SseEmitter emitter : emitters) {
            sendEvent(emitter, event);
        }
        return event;
    }

    public List<ObservedLogEvent> getHistory() {
        return new ArrayList<>(history);
    }

    public void clear() {
        history.clear();
    }

    private void trimHistory() {
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
    }

    private void sendEvent(SseEmitter emitter, ObservedLogEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.getType())
                    .data(event));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }
}
