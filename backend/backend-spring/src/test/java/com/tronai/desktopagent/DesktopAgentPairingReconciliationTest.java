package com.tronai.desktopagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.auth.JwtService;
import com.tronai.desktopagent.device.DesktopDeviceEntity;
import com.tronai.desktopagent.device.DesktopDeviceRepository;
import com.tronai.desktopagent.device.DeviceService;
import com.tronai.desktopagent.device.DeviceStatus;
import com.tronai.desktopagent.pairing.PairingRequestRepository;
import com.tronai.desktopagent.policy.Fingerprints;
import com.tronai.user.User;
import com.tronai.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DesktopAgentPairingReconciliationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository users;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DeviceService deviceService;
    @Autowired private DesktopDeviceRepository devices;
    @Autowired private PairingRequestRepository pairingRequests;

    private User owner;
    private User other;
    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void users() {
        owner = users.save(User.builder().email("repair-owner-" + UUID.randomUUID() + "@tron.ai").displayName("Owner").passwordHash("x").role("USER").enabled(true).build());
        other = users.save(User.builder().email("repair-other-" + UUID.randomUUID() + "@tron.ai").displayName("Other").passwordHash("x").role("USER").enabled(true).build());
        ownerToken = jwtService.generateAccessToken(owner.getId(), owner.getEmail(), "USER", "Owner");
        otherToken = jwtService.generateAccessToken(other.getId(), other.getEmail(), "USER", "Other");
    }

    @Test
    void normalNewDevicePairsOnce() throws Exception {
        String deviceUuid = UUID.randomUUID().toString();
        JsonNode created = createRequest(deviceUuid);
        assertThat(created.path("reconciliation").isMissingNode()).isTrue();

        JsonNode paired = pair(ownerToken, created.path("pairingCode").asText());
        assertThat(paired.path("repaired").asBoolean()).isFalse();
        assertThat(paired.path("deviceUuid").asText()).isEqualTo(deviceUuid);
        assertThat(paired.path("credential").isMissingNode() || paired.path("credential").isNull()).isTrue();

        JsonNode poll = poll(created);
        assertThat(poll.path("status").asText()).isEqualTo("PAIRED");
        assertThat(deviceService.authenticate(poll.path("credential").asText())).isPresent();
        assertThat(devices.findByDeviceUuid(deviceUuid)).isPresent();
    }

    @Test
    void sameUserWithMissingLocalCredentialRepairsWithoutADuplicateRow() throws Exception {
        String deviceUuid = UUID.randomUUID().toString();
        JsonNode first = createRequest(deviceUuid);
        JsonNode firstPair = pair(ownerToken, first.path("pairingCode").asText());
        Long deviceId = firstPair.path("id").asLong();
        String oldCredential = poll(first).path("credential").asText();
        assertThat(deviceService.authenticate(oldCredential)).isPresent();

        JsonNode repairRequest = createRequest(deviceUuid);
        assertThat(repairRequest.path("reconciliation").asText()).isEqualTo("DEVICE_REPAIR_REQUIRED");
        long requestsBeforeReconnect = pairingRequests.count();

        JsonNode repaired = pair(ownerToken, repairRequest.path("pairingCode").asText());
        assertThat(repaired.path("repaired").asBoolean()).isTrue();
        assertThat(repaired.path("id").asLong()).isEqualTo(deviceId);
        assertThat(devices.findByDeviceUuid(deviceUuid).orElseThrow().getUserId()).isEqualTo(owner.getId());

        JsonNode delivered = poll(repairRequest);
        String newCredential = delivered.path("credential").asText();
        assertThat(newCredential).isNotBlank();
        assertThat(newCredential).isNotEqualTo(oldCredential);
        assertThat(deviceService.authenticate(oldCredential)).isEmpty();
        assertThat(deviceService.authenticate(newCredential)).isPresent();

        JsonNode secondPoll = poll(repairRequest);
        assertThat(secondPoll.path("credential").isMissingNode() || secondPoll.path("credential").isNull()).isTrue();
        assertThat(deviceService.authenticate(newCredential)).isPresent();
        assertThat(pairingRequests.count()).isEqualTo(requestsBeforeReconnect);
        assertThat(devices.findByDeviceUuid(deviceUuid).orElseThrow().getId()).isEqualTo(deviceId);
    }

    @Test
    void repeatedRepairCodeStaysSingleUse() throws Exception {
        String deviceUuid = UUID.randomUUID().toString();
        JsonNode first = createRequest(deviceUuid);
        pair(ownerToken, first.path("pairingCode").asText());
        poll(first);

        JsonNode repairRequest = createRequest(deviceUuid);
        String code = repairRequest.path("pairingCode").asText();
        pair(ownerToken, code);
        String credential = poll(repairRequest).path("credential").asText();

        MvcResult reused = mockMvc.perform(post("/api/v1/desktop-agents/pair")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code, "allowedWorkspace", "C:/Dev"))))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode error = objectMapper.readTree(reused.getResponse().getContentAsString());
        assertThat(error.path("detail").asText()).contains("already been used");
        assertThat(deviceService.authenticate(credential)).isPresent();
        assertThat(devices.findByDeviceUuid(deviceUuid)).isPresent();
    }

    @Test
    void differentUserCannotTakeOwnership() throws Exception {
        String deviceUuid = UUID.randomUUID().toString();
        DesktopDeviceEntity device = devices.save(DesktopDeviceEntity.builder()
                .userId(owner.getId())
                .deviceUuid(deviceUuid)
                .displayName("owned-pc")
                .credentialHash(Fingerprints.sha256("owner-credential"))
                .status(DeviceStatus.OFFLINE)
                .allowedWorkspace("C:/Owner")
                .build());
        JsonNode created = createRequest(deviceUuid);

        MvcResult conflict = mockMvc.perform(post("/api/v1/desktop-agents/pair")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", created.path("pairingCode").asText(), "allowedWorkspace", "C:/Other"))))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode error = objectMapper.readTree(conflict.getResponse().getContentAsString());
        assertThat(error.path("code").asText()).isEqualTo("DEVICE_LINK_NOT_PROVEN");
        assertThat(error.path("detail").asText()).doesNotContain("another TRON account");
        assertThat(error.toString()).doesNotContain(owner.getEmail());
        assertThat(devices.findById(device.getId()).orElseThrow().getUserId()).isEqualTo(owner.getId());
        assertThat(deviceService.authenticate("owner-credential")).isPresent();

        JsonNode polled = poll(created);
        assertThat(polled.path("status").asText()).isEqualTo("CONFLICT");
        assertThat(polled.path("code").asText()).isEqualTo("DEVICE_LINK_NOT_PROVEN");
        assertThat(polled.toString()).doesNotContain(owner.getEmail());

        JsonNode repaired = pair(ownerToken, created.path("pairingCode").asText());
        assertThat(repaired.path("id").asLong()).isEqualTo(device.getId());
        assertThat(deviceService.authenticate("owner-credential")).isEmpty();
        assertThat(devices.findById(device.getId()).orElseThrow().getUserId()).isEqualTo(owner.getId());
    }

    @Test
    void revokedAndIncompleteOwnedDevicesRepairInPlace() throws Exception {
        String revokedUuid = UUID.randomUUID().toString();
        DesktopDeviceEntity revoked = devices.save(DesktopDeviceEntity.builder()
                .userId(owner.getId())
                .deviceUuid(revokedUuid)
                .displayName("revoked-pc")
                .credentialHash(Fingerprints.sha256("revoked-credential"))
                .status(DeviceStatus.REVOKED)
                .allowedWorkspace("C:/Old")
                .build());
        revoked.setRevokedAt(java.time.Instant.now());
        devices.save(revoked);

        String revokedHash = devices.findById(revoked.getId()).orElseThrow().getCredentialHash();
        JsonNode revokedRequest = createRequest(revokedUuid);
        assertThat(revokedRequest.path("reconciliation").asText()).isEqualTo("DEVICE_REPAIR_REQUIRED");
        JsonNode revokedPair = pair(ownerToken, revokedRequest.path("pairingCode").asText());
        assertThat(revokedPair.path("repaired").asBoolean()).isTrue();
        assertThat(revokedPair.path("id").asLong()).isEqualTo(revoked.getId());
        DesktopDeviceEntity revived = devices.findById(revoked.getId()).orElseThrow();
        assertThat(revived.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
        assertThat(revived.getRevokedAt()).isNull();
        assertThat(revived.getCredentialHash()).isNotEqualTo(revokedHash);
        assertThat(revived.getUserId()).isEqualTo(owner.getId());
        assertThat(deviceService.authenticate("revoked-credential")).isEmpty();
        String ownerCredential = poll(revokedRequest).path("credential").asText();
        assertThat(deviceService.authenticate(ownerCredential)).isPresent();

        String staleUuid = UUID.randomUUID().toString();
        DesktopDeviceEntity stale = devices.save(DesktopDeviceEntity.builder()
                .userId(owner.getId())
                .deviceUuid(staleUuid)
                .displayName("stale-pc")
                .status(DeviceStatus.PENDING)
                .allowedWorkspace("C:/Stale")
                .build());
        JsonNode staleRequest = createRequest(staleUuid);
        JsonNode stalePair = pair(ownerToken, staleRequest.path("pairingCode").asText());
        assertThat(stalePair.path("id").asLong()).isEqualTo(stale.getId());
        assertThat(devices.findByDeviceUuid(staleUuid).orElseThrow().getCredentialHash()).isNotBlank();
        assertThat(deviceService.authenticate(poll(staleRequest).path("credential").asText())).isPresent();

        JsonNode otherRevokedRequest = createRequest(revokedUuid);
        JsonNode moved = pair(otherToken, otherRevokedRequest.path("pairingCode").asText());
        assertThat(moved.path("id").asLong()).isEqualTo(revoked.getId());
        assertThat(moved.path("transferred").asBoolean()).isTrue();
        assertThat(devices.findById(revoked.getId()).orElseThrow().getUserId()).isEqualTo(other.getId());
        assertThat(devices.findByDeviceUuid(revokedUuid).orElseThrow().getId()).isEqualTo(revoked.getId());
        assertThat(deviceService.authenticate(ownerCredential)).isEmpty();
        assertThat(deviceService.authenticate(poll(otherRevokedRequest).path("credential").asText())).isPresent();
    }

    private JsonNode createRequest(String deviceUuid) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/desktop-agents/pairing-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deviceUuid", deviceUuid,
                                "publicKey", "test-public-key-" + deviceUuid,
                                "hostname", "test-pc",
                                "os", "Windows",
                                "agentVersion", "0.1.0"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString());
    }

    private JsonNode pair(String token, String code) throws Exception {
        MvcResult paired = mockMvc.perform(post("/api/v1/desktop-agents/pair")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code, "allowedWorkspace", "C:/Dev", "displayName", "Repair-PC"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(paired.getResponse().getContentAsString());
    }

    private JsonNode poll(JsonNode created) throws Exception {
        MvcResult polled = mockMvc.perform(get("/api/v1/desktop-agents/pairing-requests/" + created.path("pairingRequestId").asText())
                        .header("X-Tron-Poll-Secret", created.path("pollSecret").asText()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(polled.getResponse().getContentAsString());
    }
}
