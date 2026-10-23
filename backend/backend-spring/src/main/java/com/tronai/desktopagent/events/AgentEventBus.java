package com.tronai.desktopagent.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AgentEventBus {

    private static final Logger log = LoggerFactory.getLogger(AgentEventBus.class);

    private final ObjectMapper objectMapper;
    private final ExecutorService dispatcher = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "desktop-agent-sse");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> taskEmitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    public AgentEventBus(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribeTask(String taskId) {
        return subscribe(taskEmitters.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>()));
    }

    public SseEmitter subscribeUser(Long userId) {
        return subscribe(userEmitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()));
    }

    public void publish(Long userId, String taskId, String eventName, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Could not encode desktop agent event {}", eventName);
            return;
        }
        if (taskId != null) {
            send(taskEmitters.get(taskId), eventName, json);
        }
        if (userId != null) {
            send(userEmitters.get(userId), eventName, json);
        }
    }

    private SseEmitter subscribe(List<SseEmitter> emitters) {
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        dispatcher.execute(() -> announce(emitters, emitter));
        return emitter;
    }

    private void announce(List<SseEmitter> emitters, SseEmitter emitter) {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                Thread.sleep(40);
                emitter.send(SseEmitter.event().comment("connected"));
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                if (attempt == 4) {
                    emitters.remove(emitter);
                }
            }
        }
    }

    private void send(List<SseEmitter> emitters, String eventName, String json) {
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(emitters)) {
            dispatcher.execute(() -> deliver(emitters, emitter, SseEmitter.event().name(eventName).data(json)));
        }
    }

    private void deliver(List<SseEmitter> emitters, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (Exception e) {
            emitters.remove(emitter);
        }
    }
}
