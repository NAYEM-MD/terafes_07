package com.tronai.desktopagent.device;

import com.tronai.desktopagent.DesktopAgentException;
import com.tronai.desktopagent.connection.DeviceSessionRegistry;
import com.tronai.desktopagent.events.AgentEventBus;
import com.tronai.desktopagent.policy.Fingerprints;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DeviceService {

    private final DesktopDeviceRepository devices;
    private final DeviceSessionRegistry sessions;
    private final AgentEventBus events;

    public DeviceService(DesktopDeviceRepository devices,
                         @Lazy DeviceSessionRegistry sessions,
                         AgentEventBus events) {
        this.devices = devices;
        this.sessions = sessions;
        this.events = events;
    }

    public List<DesktopDeviceEntity> list(Long userId) {
        return devices.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public DesktopDeviceEntity requireOwned(Long userId, Long deviceId) {
        return devices.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new DesktopAgentException(HttpStatus.NOT_FOUND, "Device not found"));
    }

    public Optional<DesktopDeviceEntity> authenticate(String credential) {
        if (credential == null || credential.isBlank()) {
            return Optional.empty();
        }
        return devices.findByCredentialHash(Fingerprints.sha256(credential))
                .filter(device -> device.getStatus() != DeviceStatus.REVOKED)
                .filter(device -> device.getRevokedAt() == null);
    }

    @Transactional
    public void markOnline(Long deviceId) {
        devices.findById(deviceId).ifPresent(device -> {
            if (device.getStatus() == DeviceStatus.REVOKED) {
                return;
            }
            device.setStatus(DeviceStatus.ONLINE);
            device.setLastSeen(Instant.now());
            devices.save(device);
            publishStatus(device);
        });
    }

    @Transactional
    public void markOffline(Long deviceId) {
        devices.findById(deviceId).ifPresent(device -> {
            if (device.getStatus() == DeviceStatus.REVOKED) {
                return;
            }
            device.setStatus(DeviceStatus.OFFLINE);
            devices.save(device);
            publishStatus(device);
        });
    }

    @Transactional
    public void markBusy(Long deviceId) {
        devices.findById(deviceId).ifPresent(device -> {
            if (device.getStatus() != DeviceStatus.REVOKED) {
                device.setStatus(DeviceStatus.BUSY);
                device.setLastSeen(Instant.now());
                devices.save(device);
                publishStatus(device);
            }
        });
    }

    @Transactional
    public void markSeen(Long deviceId, String os, String hostname, String agentVersion, String workspace) {
        devices.findById(deviceId).ifPresent(device -> {
            if (device.getStatus() == DeviceStatus.REVOKED) {
                return;
            }
            device.setLastSeen(Instant.now());
            boolean becameOnline = device.getStatus() == DeviceStatus.OFFLINE || device.getStatus() == DeviceStatus.PENDING;
            if (becameOnline) {
                device.setStatus(DeviceStatus.ONLINE);
            }
            if (os != null && !os.isBlank()) {
                device.setOsName(trim(os, 100));
            }
            if (hostname != null && !hostname.isBlank()) {
                device.setHostname(trim(hostname, 255));
            }
            if (agentVersion != null && !agentVersion.isBlank()) {
                device.setAgentVersion(trim(agentVersion, 50));
            }
            if (workspace != null && !workspace.isBlank()) {
                device.setAllowedWorkspace(trim(workspace, 1024));
            }
            devices.save(device);
            if (becameOnline) {
                publishStatus(device);
            }
        });
    }

    @Transactional
    public void revoke(Long userId, Long deviceId) {
        DesktopDeviceEntity device = requireOwned(userId, deviceId);
        device.setStatus(DeviceStatus.REVOKED);
        device.setRevokedAt(Instant.now());
        device.setCredentialHash(Fingerprints.sha256("revoked:" + device.getDeviceUuid() + ":" + Instant.now()));
        device.setApprovalKeyEnvelope(null);
        devices.save(device);
        sessions.close(deviceId);
        publishStatus(device);
    }

    @Transactional
    public void refreshConnectedPresence() {
        Instant now = Instant.now();
        for (Long deviceId : sessions.connectedDeviceIds()) {
            devices.findById(deviceId).ifPresent(device -> {
                if (device.getStatus() == DeviceStatus.REVOKED) {
                    return;
                }
                device.setLastSeen(now);
                boolean restored = device.getStatus() == DeviceStatus.OFFLINE
                        || device.getStatus() == DeviceStatus.PENDING;
                if (restored) {
                    device.setStatus(DeviceStatus.ONLINE);
                }
                devices.save(device);
                if (restored) {
                    publishStatus(device);
                }
            });
        }
    }

    @Transactional
    public int markStaleOffline(Instant cutoff) {
        java.util.List<Long> candidates = devices.findAll().stream()
                .filter(device -> !sessions.isConnected(device.getId()))
                .filter(device -> device.getStatus() == DeviceStatus.ONLINE || device.getStatus() == DeviceStatus.BUSY)
                .map(DesktopDeviceEntity::getId)
                .toList();
        int changed = 0;
        for (Long deviceId : candidates) {
            if (devices.markOfflineIfStale(deviceId, cutoff) > 0) {
                devices.findById(deviceId).ifPresent(this::publishStatus);
                changed++;
            }
        }
        return changed;
    }

    public static boolean constantEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    public Map<String, Object> toView(DesktopDeviceEntity device) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", device.getId());
        view.put("deviceUuid", device.getDeviceUuid());
        view.put("displayName", device.getDisplayName());
        view.put("os", device.getOsName());
        view.put("hostname", device.getHostname());
        view.put("agentVersion", device.getAgentVersion());
        view.put("fingerprint", device.getFingerprint());
        view.put("status", device.getStatus().name());
        view.put("lastSeen", device.getLastSeen() == null ? null : device.getLastSeen().toString());
        view.put("pairedAt", device.getPairedAt() == null ? null : device.getPairedAt().toString());
        view.put("revokedAt", device.getRevokedAt() == null ? null : device.getRevokedAt().toString());
        view.put("allowedWorkspace", device.getAllowedWorkspace());
        view.put("createdAt", device.getCreatedAt() == null ? null : device.getCreatedAt().toString());
        return view;
    }

    private void publishStatus(DesktopDeviceEntity device) {
        events.publish(device.getUserId(), null, "DEVICE_STATUS", toView(device));
    }

    private static String trim(String value, int max) {
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
