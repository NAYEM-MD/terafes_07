package com.tronai.desktopagent.pairing;

import com.tronai.desktopagent.DesktopAgentException;
import com.tronai.desktopagent.DesktopAgentProperties;
import com.tronai.desktopagent.connection.DeviceSessionRegistry;
import com.tronai.desktopagent.device.DesktopDeviceEntity;
import com.tronai.desktopagent.device.DesktopDeviceRepository;
import com.tronai.desktopagent.device.DeviceService;
import com.tronai.desktopagent.device.DeviceStatus;
import com.tronai.desktopagent.policy.Fingerprints;
import com.tronai.desktopagent.policy.SecretBox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PairingService {

    static final String DEVICE_REPAIR_REQUIRED = "DEVICE_REPAIR_REQUIRED";
    static final String DEVICE_LINK_NOT_PROVEN = "DEVICE_LINK_NOT_PROVEN";
    static final String LINK_NOT_PROVEN_DETAIL = "This connection request was not started by this computer.";
    static final String REPAIR_DETAIL =
            "TRON Core already knows this device, but this installation no longer has a valid device credential.";

    private final PairingRequestRepository pairingRequests;
    private final DesktopDeviceRepository devices;
    private final DeviceSessionRegistry sessions;
    private final DesktopAgentProperties properties;
    private final String jwtSecret;
    private final String frontendUrl;
    private final String desktopDownloadUrl;
    private final SecureRandom random = new SecureRandom();
    private final AtomicInteger recentFailures = new AtomicInteger();
    private volatile long failureWindowStarted = System.currentTimeMillis();

    public PairingService(PairingRequestRepository pairingRequests,
                          DesktopDeviceRepository devices,
                          DeviceSessionRegistry sessions,
                          DesktopAgentProperties properties,
                          @Value("${jwt.secret}") String jwtSecret,
                          @Value("${TRON_FRONTEND_URL:${frontend.url:http://localhost:5174}}") String frontendUrl,
                          @Value("${TRON_DESKTOP_DOWNLOAD_WINDOWS_URL:${desktop-download.windows-url:}}") String desktopDownloadUrl) {
        this.pairingRequests = pairingRequests;
        this.devices = devices;
        this.sessions = sessions;
        this.properties = properties;
        this.jwtSecret = jwtSecret;
        this.frontendUrl = frontendUrl;
        this.desktopDownloadUrl = desktopDownloadUrl;
    }

    @Transactional
    public Map<String, Object> createRequest(String deviceUuid, String publicKey, String os,
                                              String hostname, String agentVersion, String displayName) {
        if (deviceUuid == null || !deviceUuid.matches("[0-9a-fA-F-]{36}")) {
            throw new DesktopAgentException(HttpStatus.BAD_REQUEST, "deviceUuid must be a UUID");
        }
        if (publicKey == null || publicKey.isBlank() || publicKey.length() > 8000) {
            throw new DesktopAgentException(HttpStatus.BAD_REQUEST, "publicKey is required");
        }
        String code = uniqueCode();
        String pollSecret = randomToken();
        String id = UUID.randomUUID().toString();
        PairingRequestEntity entity = PairingRequestEntity.builder()
                .id(id)
                .publicRequestId(UUID.randomUUID().toString())
                .codeHash(Fingerprints.sha256(digits(code)))
                .pollSecretHash(Fingerprints.sha256(pollSecret))
                .deviceUuid(deviceUuid)
                .publicKey(publicKey.trim())
                .fingerprint(Fingerprints.sha256(publicKey.trim()))
                .osName(trim(os, 100))
                .hostname(trim(hostname, 255))
                .agentVersion(trim(agentVersion, 50))
                .displayName(trim(displayName == null ? hostname : displayName, 255))
                .expiresAt(Instant.now().plusSeconds(properties.getPairingTtlSeconds()))
                .build();
        pairingRequests.save(entity);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pairingRequestId", id);
        body.put("pairingCode", code);
        body.put("pollSecret", pollSecret);
        body.put("expiresAt", entity.getExpiresAt().toString());
        if (devices.findByDeviceUuid(deviceUuid).isPresent()) {
            body.put("reconciliation", DEVICE_REPAIR_REQUIRED);
            body.put("detail", REPAIR_DETAIL);
        }
        return body;
    }

    @Transactional
    public Map<String, Object> createLinkRequest(String deviceUuid, String publicKey, String os,
                                                 String hostname, String agentVersion, String displayName,
                                                 String requestedWorkspace) {
        if (deviceUuid == null || !deviceUuid.matches("[0-9a-fA-F-]{36}")) {
            throw new DesktopAgentException(HttpStatus.BAD_REQUEST, "deviceUuid must be a UUID");
        }
        if (publicKey == null || publicKey.isBlank() || publicKey.length() > 8000) {
            throw new DesktopAgentException(HttpStatus.BAD_REQUEST, "publicKey is required");
        }
        String code = uniqueCode();
        String pollSecret = randomToken();
        String id = UUID.randomUUID().toString();
        String publicRequestId = UUID.randomUUID().toString();
        PairingRequestEntity entity = PairingRequestEntity.builder()
                .id(id)
                .publicRequestId(publicRequestId)
                .codeHash(Fingerprints.sha256(digits(code)))
                .pollSecretHash(Fingerprints.sha256(pollSecret))
                .deviceUuid(deviceUuid)
                .publicKey(publicKey.trim())
                .fingerprint(Fingerprints.sha256(publicKey.trim()))
                .osName(trim(os, 100))
                .hostname(trim(hostname, 255))
                .agentVersion(trim(agentVersion, 50))
                .displayName(trim(displayName == null ? hostname : displayName, 255))
                .requestedWorkspace(trim(requestedWorkspace != null && !requestedWorkspace.isBlank() ? requestedWorkspace : "C:\\Dev", 1024))
                .status("PENDING")
                .expiresAt(Instant.now().plusSeconds(properties.getPairingTtlSeconds()))
                .build();
        pairingRequests.save(entity);

        String baseFrontend = frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl.trim() : "http://localhost:5174";
        while (baseFrontend.endsWith("/")) {
            baseFrontend = baseFrontend.substring(0, baseFrontend.length() - 1);
        }
        String authorizationUrl = baseFrontend + "/connect-device?request=" + publicRequestId;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("linkRequestId", id);
        body.put("publicRequestId", publicRequestId);
        body.put("authorizationUrl", authorizationUrl);
        body.put("pollSecret", pollSecret);
        body.put("expiresAt", entity.getExpiresAt().toString());
        body.put("expiresInSeconds", properties.getPairingTtlSeconds());
        body.put("fallbackPairingCode", code);
        if (devices.findByDeviceUuid(deviceUuid).isPresent()) {
            body.put("reconciliation", DEVICE_REPAIR_REQUIRED);
            body.put("detail", REPAIR_DETAIL);
        }
        return body;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLinkRequest(String id) {
        PairingRequestEntity request = requireLinkRequest(id);

        String currentStatus = request.getStatus();
        boolean expired = request.getExpiresAt().isBefore(Instant.now())
                && !"AUTHORIZED".equals(currentStatus)
                && !"CONSUMED".equals(currentStatus);
        if (expired) {
            currentStatus = "EXPIRED";
        }
        long expiresIn = Math.max(0, request.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
        String publicId = request.getPublicRequestId() != null ? request.getPublicRequestId() : request.getId();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", publicId);
        body.put("publicRequestId", publicId);
        body.put("linkRequestId", publicId);
        body.put("deviceUuid", request.getDeviceUuid());
        body.put("displayName", request.getDisplayName());
        body.put("hostname", request.getHostname());
        body.put("os", request.getOsName());
        body.put("osName", request.getOsName());
        body.put("agentVersion", request.getAgentVersion());
        body.put("requestedWorkspace", request.getRequestedWorkspace());
        body.put("status", currentStatus);
        body.put("expiresAt", request.getExpiresAt().toString());
        body.put("expiresInSeconds", expiresIn);
        body.put("expired", expired);
        return body;
    }

    @Transactional(noRollbackFor = DesktopAgentException.class)
    public Map<String, Object> authorizeLinkRequest(Long userId, String id, String allowedWorkspaceOverride, String displayNameOverride) {
        guardFailures();
        PairingRequestEntity request = requireLinkRequest(id);

        if (request.getConsumedAt() != null || "AUTHORIZED".equals(request.getStatus()) || "CONSUMED".equals(request.getStatus())) {
            throw new DesktopAgentException(HttpStatus.CONFLICT, "Link request has already been used");
        }
        if (request.getExpiresAt().isBefore(Instant.now())) {
            throw new DesktopAgentException(HttpStatus.GONE, "Link request has expired");
        }

        DesktopDeviceEntity existing = devices.findByDeviceUuid(request.getDeviceUuid()).orElse(null);
        if (!desktopHoldsRegisteredKey(existing, userId, request.getPublicKey())) {
            request.setConflictCode(DEVICE_LINK_NOT_PROVEN);
            request.setStatus("DENIED");
            pairingRequests.save(request);
            throw new DesktopAgentException(HttpStatus.CONFLICT, LINK_NOT_PROVEN_DETAIL, DEVICE_LINK_NOT_PROVEN);
        }

        String targetWorkspace = allowedWorkspaceOverride != null && !allowedWorkspaceOverride.isBlank()
                ? allowedWorkspaceOverride
                : request.getRequestedWorkspace();
        if (targetWorkspace == null || targetWorkspace.isBlank()) {
            targetWorkspace = "C:\\Dev";
        }

        String credential = randomToken();
        String approvalKey = randomToken();
        boolean repair = existing != null;
        boolean transferred = existing != null && !existing.getUserId().equals(userId);
        DesktopDeviceEntity device = repair ? existing : DesktopDeviceEntity.builder()
                .userId(userId)
                .deviceUuid(request.getDeviceUuid())
                .build();
        device.setUserId(userId);
        device.setDisplayName(trim(displayNameOverride != null && !displayNameOverride.isBlank()
                ? displayNameOverride : request.getDisplayName(), 255));
        device.setOsName(request.getOsName());
        device.setHostname(request.getHostname());
        device.setAgentVersion(request.getAgentVersion());
        device.setPublicKey(request.getPublicKey());
        device.setFingerprint(request.getFingerprint());
        device.setCredentialHash(Fingerprints.sha256(credential));
        device.setApprovalKeyEnvelope(SecretBox.seal(jwtSecret, "approval:" + request.getDeviceUuid(), approvalKey));
        device.setStatus(DeviceStatus.OFFLINE);
        device.setPairedAt(Instant.now());
        device.setRevokedAt(null);
        device.setAllowedWorkspace(trim(targetWorkspace, 1024));
        if (device.getDisplayName() == null || device.getDisplayName().isBlank()) {
            device.setDisplayName("TRON Desktop");
        }
        device = devices.save(device);
        sessions.close(device.getId());

        request.setConflictCode(null);
        request.setConsumedAt(Instant.now());
        request.setStatus("AUTHORIZED");
        request.setDeviceId(device.getId());
        request.setCredentialEnvelope(SecretBox.seal(jwtSecret, "credential:" + request.getId(), credential));
        request.setApprovalKeyEnvelope(SecretBox.seal(jwtSecret, "approval-delivery:" + request.getId(), approvalKey));
        pairingRequests.save(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", device.getId());
        body.put("deviceUuid", device.getDeviceUuid());
        body.put("displayName", device.getDisplayName());
        body.put("status", device.getStatus().name());
        body.put("allowedWorkspace", device.getAllowedWorkspace());
        body.put("fingerprint", device.getFingerprint());
        body.put("pairedAt", device.getPairedAt().toString());
        body.put("repaired", repair);
        body.put("transferred", transferred);
        return body;
    }

    public Map<String, Object> getDownloadInfo() {
        boolean available = desktopDownloadUrl != null && !desktopDownloadUrl.isBlank();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("available", available);
        body.put("downloadUrl", available ? desktopDownloadUrl.trim() : null);
        body.put("platform", "windows");
        return body;
    }

    @Transactional(noRollbackFor = DesktopAgentException.class)
    public Map<String, Object> pair(Long userId, String code, String allowedWorkspace, String displayName) {
        guardFailures();
        if (allowedWorkspace == null || allowedWorkspace.isBlank()) {
            throw new DesktopAgentException(HttpStatus.BAD_REQUEST, "allowedWorkspace is required");
        }
        String normalized = digits(code);
        if (normalized.length() != 6) {
            registerFailure();
            throw new DesktopAgentException(HttpStatus.BAD_REQUEST, "Pairing code is invalid");
        }
        PairingRequestEntity request = pairingRequests.findByCodeHash(Fingerprints.sha256(normalized))
                .orElseThrow(() -> {
                    registerFailure();
                    return new DesktopAgentException(HttpStatus.BAD_REQUEST, "Pairing code is invalid");
                });
        if (request.getConsumedAt() != null) {
            throw new DesktopAgentException(HttpStatus.CONFLICT, "Pairing code has already been used");
        }
        if (request.getExpiresAt().isBefore(Instant.now())) {
            throw new DesktopAgentException(HttpStatus.GONE, "Pairing code has expired");
        }
        DesktopDeviceEntity existing = devices.findByDeviceUuid(request.getDeviceUuid()).orElse(null);
        if (!desktopHoldsRegisteredKey(existing, userId, request.getPublicKey())) {
            request.setConflictCode(DEVICE_LINK_NOT_PROVEN);
            pairingRequests.save(request);
            throw new DesktopAgentException(HttpStatus.CONFLICT, LINK_NOT_PROVEN_DETAIL, DEVICE_LINK_NOT_PROVEN);
        }

        String credential = randomToken();
        String approvalKey = randomToken();
        boolean repair = existing != null;
        boolean transferred = existing != null && !existing.getUserId().equals(userId);
        DesktopDeviceEntity device = repair ? existing : DesktopDeviceEntity.builder()
                .userId(userId)
                .deviceUuid(request.getDeviceUuid())
                .build();
        device.setUserId(userId);
        device.setDisplayName(trim(displayName == null || displayName.isBlank() ? request.getDisplayName() : displayName, 255));
        device.setOsName(request.getOsName());
        device.setHostname(request.getHostname());
        device.setAgentVersion(request.getAgentVersion());
        device.setPublicKey(request.getPublicKey());
        device.setFingerprint(request.getFingerprint());
        device.setCredentialHash(Fingerprints.sha256(credential));
        device.setApprovalKeyEnvelope(SecretBox.seal(jwtSecret, "approval:" + request.getDeviceUuid(), approvalKey));
        device.setStatus(DeviceStatus.OFFLINE);
        device.setPairedAt(Instant.now());
        device.setRevokedAt(null);
        device.setAllowedWorkspace(trim(allowedWorkspace, 1024));
        if (device.getDisplayName() == null || device.getDisplayName().isBlank()) {
            device.setDisplayName("TRON Desktop");
        }
        device = devices.save(device);
        sessions.close(device.getId());

        request.setConflictCode(null);
        request.setConsumedAt(Instant.now());
        request.setDeviceId(device.getId());
        request.setCredentialEnvelope(SecretBox.seal(jwtSecret, "credential:" + request.getId(), credential));
        request.setApprovalKeyEnvelope(SecretBox.seal(jwtSecret, "approval-delivery:" + request.getId(), approvalKey));
        pairingRequests.save(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", device.getId());
        body.put("deviceUuid", device.getDeviceUuid());
        body.put("displayName", device.getDisplayName());
        body.put("status", device.getStatus().name());
        body.put("allowedWorkspace", device.getAllowedWorkspace());
        body.put("fingerprint", device.getFingerprint());
        body.put("pairedAt", device.getPairedAt().toString());
        body.put("repaired", repair);
        body.put("transferred", transferred);
        return body;
    }

    @Transactional
    public Map<String, Object> poll(String pairingRequestId, String pollSecret) {
        if (pairingRequestId == null || pollSecret == null || pollSecret.isBlank()) {
            throw new DesktopAgentException(HttpStatus.UNAUTHORIZED, "Pairing poll secret is required");
        }
        String secretHash = Fingerprints.sha256(pollSecret);
        PairingRequestEntity request = pairingRequests.findByIdAndPollSecretHash(pairingRequestId, secretHash)
                .or(() -> pairingRequests.findByPublicRequestId(pairingRequestId)
                        .filter(found -> secretHash.equals(found.getPollSecretHash())))
                .orElseThrow(() -> new DesktopAgentException(HttpStatus.NOT_FOUND, "Pairing request not found"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pairingRequestId", request.getId());
        if (request.getConflictCode() != null && request.getConsumedAt() == null) {
            body.put("status", "CONFLICT");
            body.put("code", request.getConflictCode());
            body.put("detail", conflictDetail(request.getConflictCode()));
            return body;
        }
        if (request.getExpiresAt().isBefore(Instant.now()) && request.getConsumedAt() == null) {
            body.put("status", "EXPIRED");
            return body;
        }
        if (request.getConsumedAt() == null) {
            body.put("status", "PENDING");
            body.put("expiresAt", request.getExpiresAt().toString());
            return body;
        }
        body.put("status", "PAIRED");
        body.put("deviceId", request.getDeviceId());
        body.put("deviceUuid", request.getDeviceUuid());
        if (request.getDeviceId() != null) {
            devices.findById(request.getDeviceId()).ifPresent(d -> {
                body.put("allowedWorkspace", d.getAllowedWorkspace());
                body.put("displayName", d.getDisplayName());
            });
        }
        if (request.getCredentialDeliveredAt() == null && request.getCredentialEnvelope() != null) {
            String credential = SecretBox.open(jwtSecret, "credential:" + request.getId(), request.getCredentialEnvelope());
            String approvalKey = SecretBox.open(jwtSecret, "approval-delivery:" + request.getId(), request.getApprovalKeyEnvelope());
            request.setCredentialEnvelope(null);
            request.setApprovalKeyEnvelope(null);
            request.setCredentialDeliveredAt(Instant.now());
            request.setStatus("CONSUMED");
            pairingRequests.save(request);
            body.put("deviceId", request.getDeviceId());
            body.put("deviceCredential", credential);
            body.put("credential", credential);
            body.put("approvalKey", approvalKey);
            if (request.getDeviceId() != null) {
                devices.findById(request.getDeviceId()).ifPresent(device -> body.put("allowedWorkspace", device.getAllowedWorkspace()));
            }
        }
        return body;
    }

    private PairingRequestEntity requireLinkRequest(String id) {
        if (id == null || id.isBlank()) {
            throw new DesktopAgentException(HttpStatus.BAD_REQUEST, "linkRequestId is required");
        }
        return pairingRequests.findByPublicRequestId(id)
                .or(() -> pairingRequests.findById(id))
                .orElseThrow(() -> new DesktopAgentException(HttpStatus.NOT_FOUND, "Device link request not found"));
    }

    /**
     * A different account may take the existing row only when this request was
     * created with the public key already registered for that computer.
     * Knowing the UUID is not enough.
     */
    private static boolean desktopHoldsRegisteredKey(DesktopDeviceEntity existing, Long userId, String requestPublicKey) {
        if (existing == null || existing.getUserId().equals(userId)) {
            return true;
        }
        String registered = existing.getPublicKey();
        if (registered == null || registered.isBlank() || requestPublicKey == null || requestPublicKey.isBlank()) {
            return false;
        }
        return DeviceService.constantEquals(registered.trim(), requestPublicKey.trim());
    }

    private static String conflictDetail(String code) {
        if (DEVICE_LINK_NOT_PROVEN.equals(code)) {
            return LINK_NOT_PROVEN_DETAIL;
        }
        return REPAIR_DETAIL;
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < 8; attempt++) {
            int value = random.nextInt(1_000_000);
            String digits = String.format("%06d", value);
            String code = digits.substring(0, 3) + "-" + digits.substring(3);
            if (pairingRequests.findByCodeHash(Fingerprints.sha256(digits)).isEmpty()) {
                return code;
            }
        }
        throw new DesktopAgentException(HttpStatus.SERVICE_UNAVAILABLE, "Could not allocate a pairing code");
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void guardFailures() {
        long now = System.currentTimeMillis();
        if (now - failureWindowStarted > 600_000) {
            failureWindowStarted = now;
            recentFailures.set(0);
        }
        if (recentFailures.get() > 30) {
            throw new DesktopAgentException(HttpStatus.TOO_MANY_REQUESTS, "Too many pairing attempts");
        }
    }

    private void registerFailure() {
        recentFailures.incrementAndGet();
    }

    static String digits(String code) {
        if (code == null) {
            return "";
        }
        return code.replace("-", "").replace(" ", "").trim();
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
