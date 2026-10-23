package com.tronai.desktopagent.device;

import com.tronai.auth.SecurityUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/desktop-agents/devices")
public class DeviceController {

    private final DeviceService devices;

    public DeviceController(DeviceService devices) {
        this.devices = devices;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return devices.list(SecurityUtils.getRequiredUserId()).stream().map(devices::toView).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return devices.toView(devices.requireOwned(SecurityUtils.getRequiredUserId(), id));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> revoke(@PathVariable Long id) {
        Long userId = SecurityUtils.getRequiredUserId();
        devices.revoke(userId, id);
        return devices.toView(devices.requireOwned(userId, id));
    }
}
