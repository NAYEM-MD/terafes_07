package com.tronai.desktopagent.device;

import com.tronai.desktopagent.DesktopAgentProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DevicePresenceMonitor {

    private final DeviceService devices;
    private final DesktopAgentProperties properties;

    public DevicePresenceMonitor(DeviceService devices, DesktopAgentProperties properties) {
        this.devices = devices;
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 15000)
    public void markStaleDevicesOffline() {
        devices.refreshConnectedPresence();
        Instant cutoff = Instant.now().minusSeconds(properties.getHeartbeatTimeoutSeconds());
        devices.markStaleOffline(cutoff);
    }
}
