package com.tronai.desktopagent.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.desktopagent.protocol.ProtocolMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(DeviceSessionRegistry.class);
    private static final int MAX_BUFFER = 64_000;

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Session> sessions = new ConcurrentHashMap<>();

    public DeviceSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long deviceId, String deviceUuid, Long userId, WebSocketSession socket) {
        Session previous = sessions.put(deviceId, new Session(socket, deviceUuid, userId));
        if (previous != null) {
            previous.failPending("Device reconnected");
            closeQuietly(previous.socket);
        }
    }

    public void remove(Long deviceId, WebSocketSession socket) {
        Session session = sessions.get(deviceId);
        if (session != null && session.socket.getId().equals(socket.getId())) {
            sessions.remove(deviceId);
            session.failPending("Device disconnected");
        }
    }

    public boolean isConnected(Long deviceId) {
        Session session = sessions.get(deviceId);
        return session != null && session.socket.isOpen();
    }

    public java.util.Set<Long> connectedDeviceIds() {
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        sessions.forEach((id, session) -> {
            if (session.socket.isOpen()) {
                ids.add(id);
            }
        });
        return ids;
    }

    public Session require(Long deviceId) {
        Session session = sessions.get(deviceId);
        if (session == null || !session.socket.isOpen()) {
            throw new IllegalStateException("Device is offline");
        }
        return session;
    }

    public void send(Long deviceId, ProtocolMessage message) {
        Session session = require(deviceId);
        send(session, message);
    }

    public CompletableFuture<ProtocolMessage.Result> openAction(Long deviceId, String actionId, String taskId) {
        Session session = require(deviceId);
        Pending pending = new Pending(taskId);
        session.pending.put(actionId, pending);
        return pending.future;
    }

    public void appendOutput(Long deviceId, String actionId, String stream, String chunk) {
        Session session = sessions.get(deviceId);
        if (session == null || chunk == null) {
            return;
        }
        Pending pending = session.pending.get(actionId);
        if (pending == null) {
            return;
        }
        StringBuilder buffer = "stderr".equalsIgnoreCase(stream) ? pending.stderr : pending.stdout;
        synchronized (buffer) {
            if (buffer.length() < MAX_BUFFER) {
                buffer.append(chunk);
            }
        }
    }

    public void completeAction(Long deviceId, String actionId, ProtocolMessage.Result result) {
        Session session = sessions.get(deviceId);
        if (session == null) {
            return;
        }
        Pending pending = session.pending.remove(actionId);
        if (pending == null) {
            return;
        }
        if (result.getStdout() == null) {
            result.setStdout(pending.stdout.toString());
        }
        if (result.getStderr() == null) {
            result.setStderr(pending.stderr.toString());
        }
        pending.future.complete(result);
    }

    public void cancelActions(Long deviceId) {
        Session session = sessions.get(deviceId);
        if (session != null) {
            session.failPending("Task cancelled");
        }
    }

    public void close(Long deviceId) {
        Session session = sessions.remove(deviceId);
        if (session != null) {
            session.failPending("Device revoked");
            closeQuietly(session.socket);
        }
    }

    public Long userId(Long deviceId) {
        Session session = sessions.get(deviceId);
        return session == null ? null : session.userId;
    }

    private void send(Session session, ProtocolMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            synchronized (session.socket) {
                if (session.socket.isOpen()) {
                    session.socket.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.warn("Could not send desktop agent message type {}", message.getType());
            throw new IllegalStateException("Could not reach the desktop agent");
        }
    }

    private void closeQuietly(WebSocketSession socket) {
        try {
            if (socket.isOpen()) {
                socket.close();
            }
        } catch (Exception ignored) {
            log.debug("Desktop socket already closed");
        }
    }

    public static final class Session {
        private final WebSocketSession socket;
        private final String deviceUuid;
        private final Long userId;
        private final Map<String, Pending> pending = new ConcurrentHashMap<>();
        private volatile boolean helloSeen;

        private Session(WebSocketSession socket, String deviceUuid, Long userId) {
            this.socket = socket;
            this.deviceUuid = deviceUuid;
            this.userId = userId;
        }

        public String deviceUuid() {
            return deviceUuid;
        }

        public Long userId() {
            return userId;
        }

        public boolean helloSeen() {
            return helloSeen;
        }

        public void markHello() {
            this.helloSeen = true;
        }

        private void failPending(String reason) {
            pending.forEach((actionId, pendingAction) -> {
                ProtocolMessage.Result result = new ProtocolMessage.Result();
                result.setSuccess(false);
                result.setCancelled(true);
                result.setSummary(reason);
                pendingAction.future.complete(result);
            });
            pending.clear();
        }
    }

    private static final class Pending {
        private final String taskId;
        private final CompletableFuture<ProtocolMessage.Result> future = new CompletableFuture<>();
        private final StringBuilder stdout = new StringBuilder();
        private final StringBuilder stderr = new StringBuilder();

        private Pending(String taskId) {
            this.taskId = taskId;
        }
    }
}
