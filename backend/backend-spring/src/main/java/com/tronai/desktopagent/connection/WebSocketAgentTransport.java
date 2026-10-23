package com.tronai.desktopagent.connection;

import com.tronai.desktopagent.protocol.ProtocolMessage;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class WebSocketAgentTransport {

    private final DeviceSessionRegistry sessions;

    public WebSocketAgentTransport(DeviceSessionRegistry sessions) {
        this.sessions = sessions;
    }

    public boolean isConnected(Long deviceId) {
        return sessions.isConnected(deviceId);
    }

    public void send(Long deviceId, ProtocolMessage message) {
        sessions.send(deviceId, message);
    }

    public CompletableFuture<ProtocolMessage.Result> requestAction(Long deviceId, ProtocolMessage message) {
        CompletableFuture<ProtocolMessage.Result> future = sessions.openAction(deviceId, message.getActionId(), message.getTaskId());
        sessions.send(deviceId, message);
        return future;
    }

    public void cancel(Long deviceId) {
        sessions.cancelActions(deviceId);
    }
}
