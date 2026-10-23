package com.tronai.desktopagent.connection;

import com.tronai.desktopagent.device.DesktopDeviceEntity;
import com.tronai.desktopagent.device.DeviceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.ServerContainer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

@Component
public class DeviceHandshakeInterceptor implements HandshakeInterceptor {

    private final DeviceService deviceService;

    public DeviceHandshakeInterceptor(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        HttpServletRequest http = servletRequest.getServletRequest();
        String header = http.getHeader("Authorization");
        if (header == null || !header.startsWith("Device ")) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        String credential = header.substring("Device ".length()).trim();
        Optional<DesktopDeviceEntity> device = deviceService.authenticate(credential);
        if (device.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put("deviceDbId", device.get().getId());
        attributes.put("deviceUuid", device.get().getDeviceUuid());
        attributes.put("userId", device.get().getUserId());
        Object container = http.getServletContext().getAttribute(ServerContainer.class.getName());
        if (container instanceof ServerContainer serverContainer) {
            serverContainer.setDefaultMaxTextMessageBufferSize(2 * 1024 * 1024);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // The device credential is not logged.
    }
}
