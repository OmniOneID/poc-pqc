package org.omnione.did.poc.pqc.server.util;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class LogStreamService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Set<String> blacklist = new CopyOnWriteArraySet<>(Set.of("/", "/api/entity-registration"));

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void broadcast(String type, String message) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(type).data(message));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    public boolean isBlacklisted(String uri) {
        return blacklist.contains(uri);
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }

    public void addBlacklist(String uri) {
        blacklist.add(uri);
    }

    public void removeBlacklist(String uri) {
        blacklist.remove(uri);
    }
}
