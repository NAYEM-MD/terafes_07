package com.tronai.desktopagent.connection;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class DesktopAgentWebSocketConfig implements WebSocketConfigurer {

    private final DesktopAgentWebSocketHandler handler;
    private final DeviceHandshakeInterceptor interceptor;

    public DesktopAgentWebSocketConfig(DesktopAgentWebSocketHandler handler,
                                       DeviceHandshakeInterceptor interceptor) {
        this.handler = handler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/v1/desktop-agent/ws")
                .addInterceptors(interceptor)
                .setAllowedOrigins("*");
    }
}
