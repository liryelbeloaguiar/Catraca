package com.queueflow.queue;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class DisplayPanelStreamService {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> connections =
            new ConcurrentHashMap<>();

    public SseEmitter connect(UUID publicToken) {
        SseEmitter emitter = new SseEmitter(0L);
        connections.computeIfAbsent(publicToken, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable remove = () -> remove(publicToken, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data("ready"));
        } catch (IOException exception) {
            remove.run();
        }
        return emitter;
    }

    public void refresh(UUID publicToken) {
        var emitters = connections.get(publicToken);
        if (emitters == null) return;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("refresh").data(System.currentTimeMillis()));
            } catch (IOException exception) {
                remove(publicToken, emitter);
            }
        }
    }

    private void remove(UUID publicToken, SseEmitter emitter) {
        var emitters = connections.get(publicToken);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) connections.remove(publicToken, emitters);
    }
}
