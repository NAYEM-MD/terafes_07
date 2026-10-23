package com.tronai.desktopagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.auth.AuthService;
import com.tronai.auth.JwtService;
import com.tronai.desktopagent.device.DesktopDeviceEntity;
import com.tronai.desktopagent.device.DesktopDeviceRepository;
import com.tronai.desktopagent.device.DeviceService;
import com.tronai.desktopagent.device.DeviceStatus;
import com.tronai.desktopagent.pairing.PairingService;
import com.tronai.desktopagent.approval.AgentApprovalEntity;
import com.tronai.desktopagent.approval.AgentApprovalRepository;
import com.tronai.desktopagent.approval.ApprovalStatus;
import com.tronai.desktopagent.connection.DeviceSessionRegistry;
import com.tronai.desktopagent.task.AgentTaskEntity;
import com.tronai.desktopagent.task.AgentTaskRepository;
import com.tronai.desktopagent.task.ControlMode;
import com.tronai.desktopagent.task.TaskStatus;
import com.tronai.user.User;
import com.tronai.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeviceLinkIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DesktopDeviceRepository deviceRepository;
    @Autowired private DeviceService deviceService;
    @Autowired private PairingService pairingService;
    @Autowired private JwtService jwtService;
    @Autowired private AuthService authService;
    @Autowired private DeviceSessionRegistry sessions;
    @Autowired private AgentTaskRepository taskRepository;
    @Autowired private AgentApprovalRepository approvalRepository;

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .displayName(email.split("@")[0])
                .role("USER")
                .enabled(true)
                .build());
    }

    private String userToken(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), user.getDisplayName());
    }

    @Test
    @DisplayName("1. Complete browser device link request, authorization, and single-use credential delivery")
    void browserDeviceLinkLifecycle() throws Exception {
        String deviceUuid = UUID.randomUUID().toString();
        String pubKey = "-----BEGIN PUBLIC KEY-----\nMCowBQYDK2VwAyEAtest123\n-----END PUBLIC KEY-----\n";

        // Step 1: Desktop requests link session
        MvcResult createResult = mockMvc.perform(post("/api/v1/desktop-agents/link-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deviceUuid", deviceUuid,
                                "publicKey", pubKey,
                                "os", "Windows 11",
                                "hostname", "DESKTOP-TEST",
                                "agentVersion", "0.1.0",
                                "displayName", "My PC",
                                "requestedWorkspace", "C:\\Dev\\Project"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkRequestId").exists())
                .andExpect(jsonPath("$.authorizationUrl").exists())
                .andExpect(jsonPath("$.pollSecret").exists())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String linkRequestId = created.path("linkRequestId").asText();
        String publicRequestId = created.path("publicRequestId").asText();
        String authUrl = created.path("authorizationUrl").asText();
        String pollSecret = created.path("pollSecret").asText();

        assertThat(publicRequestId).isNotBlank();
        assertThat(publicRequestId).isNotEqualTo(linkRequestId);
        assertThat(authUrl).contains("/connect-device?request=" + publicRequestId);
        assertThat(authUrl).doesNotContain(pollSecret);
        assertThat(authUrl).doesNotContain(linkRequestId);
        assertThat(authUrl).doesNotContain("credential");

        mockMvc.perform(get("/api/v1/desktop-agents/link-requests/" + publicRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicRequestId").value(publicRequestId))
                .andExpect(jsonPath("$.displayName").value("My PC"))
                .andExpect(jsonPath("$.os").value("Windows 11"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.pollSecret").doesNotExist());

        mockMvc.perform(get("/api/v1/desktop-agents/link-requests/" + linkRequestId + "/poll")
                        .header("X-Tron-Poll-Secret", pollSecret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        User user = createUser("pc-owner-" + UUID.randomUUID() + "@tron.ai");
        String token = userToken(user);

        MvcResult authResult = mockMvc.perform(post("/api/v1/desktop-agents/link-requests/" + publicRequestId + "/authorize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "allowedWorkspace", "C:\\Dev\\Project"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceUuid").value(deviceUuid))
                .andExpect(jsonPath("$.allowedWorkspace").value("C:\\Dev\\Project"))
                .andReturn();

        long deviceId = objectMapper.readTree(authResult.getResponse().getContentAsString()).path("id").asLong();

        MvcResult poll1 = mockMvc.perform(post("/api/v1/desktop-agents/link-requests/" + linkRequestId + "/poll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pollSecret", pollSecret))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAIRED"))
                .andExpect(jsonPath("$.deviceCredential").exists())
                .andExpect(jsonPath("$.credential").exists())
                .andExpect(jsonPath("$.approvalKey").exists())
                .andExpect(jsonPath("$.allowedWorkspace").value("C:\\Dev\\Project"))
                .andReturn();

        JsonNode delivered = objectMapper.readTree(poll1.getResponse().getContentAsString());
        String credential = delivered.path("deviceCredential").asText();
        assertThat(credential).isEqualTo(delivered.path("credential").asText());
        assertThat(credential).isNotBlank();

        DesktopDeviceEntity authed = deviceService.authenticate(credential).orElseThrow();
        assertThat(authed.getId()).isEqualTo(deviceId);

        mockMvc.perform(get("/api/v1/desktop-agents/link-requests/" + linkRequestId + "/poll")
                        .header("X-Tron-Poll-Secret", pollSecret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAIRED"))
                .andExpect(jsonPath("$.deviceCredential").doesNotExist())
                .andExpect(jsonPath("$.credential").doesNotExist())
                .andExpect(jsonPath("$.approvalKey").doesNotExist());
    }

    @Test
    @DisplayName("2. Multi-device support: same user can own multiple computers with isolated credentials")
    void sameUserCanOwnMultipleComputers() throws Exception {
        User user = createUser("multi-pc-" + UUID.randomUUID() + "@tron.ai");
        String token = userToken(user);

        // Computer A
        String uuidA = UUID.randomUUID().toString();
        var linkA = pairingService.createLinkRequest(uuidA, "pub-key-a", "Windows 11", "HOST-A", "0.1.0", "PC-A", "C:\\DevA");
        pairingService.authorizeLinkRequest(user.getId(), (String) linkA.get("linkRequestId"), "C:\\DevA", "PC-A");
        var pollA = pairingService.poll((String) linkA.get("linkRequestId"), (String) linkA.get("pollSecret"));

        // Computer B
        String uuidB = UUID.randomUUID().toString();
        var linkB = pairingService.createLinkRequest(uuidB, "pub-key-b", "Linux", "HOST-B", "0.1.0", "PC-B", "/home/dev");
        pairingService.authorizeLinkRequest(user.getId(), (String) linkB.get("linkRequestId"), "/home/dev", "PC-B");
        var pollB = pairingService.poll((String) linkB.get("linkRequestId"), (String) linkB.get("pollSecret"));

        // Both devices appear in user's device list
        mockMvc.perform(get("/api/v1/desktop-agents/devices")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(pollA.get("deviceCredential")).isEqualTo(pollA.get("credential"));
        assertThat(pollA.get("credential")).isNotEqualTo(pollB.get("credential"));
        assertThat(uuidA).isNotEqualTo(uuidB);
        assertThat(deviceRepository.findByDeviceUuid(uuidA).orElseThrow().getAllowedWorkspace()).isEqualTo("C:\\DevA");
        assertThat(deviceRepository.findByDeviceUuid(uuidB).orElseThrow().getAllowedWorkspace()).isEqualTo("/home/dev");
    }

    @Test
    @DisplayName("3. Cross-account isolation: User B cannot see or steal User A's device")
    void crossAccountIsolation() throws Exception {
        User userA = createUser("user-a-" + UUID.randomUUID() + "@tron.ai");
        User userB = createUser("user-b-" + UUID.randomUUID() + "@tron.ai");

        String uuidA = UUID.randomUUID().toString();
        var linkA = pairingService.createLinkRequest(uuidA, "pub-key-a", "Windows 11", "HOST-A", "0.1.0", "PC-A", "C:\\Dev");
        var authorized = pairingService.authorizeLinkRequest(userA.getId(), (String) linkA.get("linkRequestId"), "C:\\Dev", "PC-A");
        Long deviceIdA = (Long) authorized.get("id");

        // User B queries device list -> empty
        mockMvc.perform(get("/api/v1/desktop-agents/devices")
                        .header("Authorization", "Bearer " + userToken(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // User B attempts direct deletion/revocation of User A's device -> 404
        mockMvc.perform(delete("/api/v1/desktop-agents/devices/" + deviceIdA)
                        .header("Authorization", "Bearer " + userToken(userB)))
                .andExpect(status().isNotFound());

        // A UUID alone cannot move the computer. The desktop public key is required.
        var manualAttempt = pairingService.createLinkRequest(uuidA, "someone-elses-key", "Windows 11", "HOST-A", "0.1.0", "PC-A", "C:\\Dev");
        mockMvc.perform(post("/api/v1/desktop-agents/link-requests/" + manualAttempt.get("publicRequestId") + "/authorize")
                        .header("Authorization", "Bearer " + userToken(userB)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEVICE_LINK_NOT_PROVEN"));
        assertThat(deviceRepository.findById(deviceIdA).orElseThrow().getUserId()).isEqualTo(userA.getId());

        mockMvc.perform(get("/api/v1/desktop-agents/devices/" + deviceIdA)
                        .header("Authorization", "Bearer " + userToken(userB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/desktop-agents/devices/" + deviceIdA)
                        .header("Authorization", "Bearer " + userToken(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceUuid").value(uuidA));

        mockMvc.perform(post("/api/v1/agent-tasks")
                        .header("Authorization", "Bearer " + userToken(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deviceId", deviceIdA,
                                "goal", "run on someone else's computer",
                                "workspace", "C:\\Dev"
                        ))))
                .andExpect(status().isNotFound());

        String taskId = UUID.randomUUID().toString();
        taskRepository.save(AgentTaskEntity.builder()
                .id(taskId)
                .userId(userA.getId())
                .deviceId(deviceIdA)
                .goal("owned task")
                .status(TaskStatus.WAITING_APPROVAL)
                .controlMode(ControlMode.AGENT)
                .workspace("C:\\Dev")
                .actionCount(0)
                .needsReinspect(false)
                .build());
        String approvalId = UUID.randomUUID().toString();
        approvalRepository.save(AgentApprovalEntity.builder()
                .id(approvalId)
                .userId(userA.getId())
                .deviceId(deviceIdA)
                .taskId(taskId)
                .actionId(UUID.randomUUID().toString())
                .fingerprint("abc")
                .risk("HIGH")
                .summary("confirm")
                .status(ApprovalStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(300))
                .build());
        mockMvc.perform(post("/api/v1/agent-tasks/" + taskId + "/approvals/" + approvalId + "/approve")
                        .header("Authorization", "Bearer " + userToken(userB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/agent-tasks/" + taskId)
                        .header("Authorization", "Bearer " + userToken(userB)))
                .andExpect(status().isNotFound());

        DesktopDeviceEntity verify = deviceRepository.findById(deviceIdA).orElseThrow();
        assertThat(verify.getUserId()).isEqualTo(userA.getId());

        // A fresh desktop link request carries the computer's registered public key.
        // User B can move that same row without User A's password or a revoke.
        var pollA = pairingService.poll((String) linkA.get("linkRequestId"), (String) linkA.get("pollSecret"));
        String oldCredential = (String) pollA.get("deviceCredential");
        var transferRequest = pairingService.createLinkRequest(uuidA, "pub-key-a", "Windows 11", "HOST-A", "0.1.0", "PC-A", "C:\\Work");
        var transferred = pairingService.authorizeLinkRequest(userB.getId(), (String) transferRequest.get("publicRequestId"), "C:\\Work", "PC-A");
        assertThat(transferred.get("id")).isEqualTo(deviceIdA);
        assertThat(transferred.get("deviceUuid")).isEqualTo(uuidA);
        assertThat(transferred.get("transferred")).isEqualTo(true);
        assertThat(deviceRepository.findByDeviceUuid(uuidA).orElseThrow().getId()).isEqualTo(deviceIdA);

        var transferPoll = pairingService.poll((String) transferRequest.get("linkRequestId"), (String) transferRequest.get("pollSecret"));
        String newCredential = (String) transferPoll.get("deviceCredential");
        assertThat(newCredential).isNotBlank();
        assertThat(newCredential).isNotEqualTo(oldCredential);
        assertThat(deviceService.authenticate(oldCredential)).isEmpty();
        assertThat(deviceService.authenticate(newCredential)).isPresent();
        assertThat(pairingService.poll((String) transferRequest.get("linkRequestId"), (String) transferRequest.get("pollSecret")).get("deviceCredential")).isNull();

        DesktopDeviceEntity moved = deviceRepository.findById(deviceIdA).orElseThrow();
        assertThat(moved.getUserId()).isEqualTo(userB.getId());
        assertThat(moved.getDeviceUuid()).isEqualTo(uuidA);
        assertThat(moved.getAllowedWorkspace()).isEqualTo("C:\\Work");

        mockMvc.perform(get("/api/v1/desktop-agents/devices")
                        .header("Authorization", "Bearer " + userToken(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/desktop-agents/devices")
                        .header("Authorization", "Bearer " + userToken(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceUuid").value(uuidA));
        mockMvc.perform(get("/api/v1/desktop-agents/devices/" + deviceIdA)
                        .header("Authorization", "Bearer " + userToken(userA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/desktop-agents/devices/" + deviceIdA)
                        .header("Authorization", "Bearer " + userToken(userA)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. Web logout does not invalidate Desktop Agent device credentials")
    void webLogoutDoesNotDisconnectComputer() throws Exception {
        User user = createUser("logout-test-" + UUID.randomUUID() + "@tron.ai");
        AuthService.AuthResult loginResult = authService.createAuthResult(user);
        String refreshToken = loginResult.cookie().getValue();

        String uuid = UUID.randomUUID().toString();
        var link = pairingService.createLinkRequest(uuid, "pub-key", "Windows 11", "PC", "0.1.0", "PC", "C:\\Dev");
        pairingService.authorizeLinkRequest(user.getId(), (String) link.get("linkRequestId"), "C:\\Dev", "PC");
        var poll = pairingService.poll((String) link.get("linkRequestId"), (String) link.get("pollSecret"));
        String deviceCredential = (String) poll.get("deviceCredential");
        assertThat(deviceCredential).isEqualTo(poll.get("credential"));

        Long deviceId = (Long) poll.get("deviceId");
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.isOpen()).thenReturn(true);
        when(socket.getId()).thenReturn("desktop-session");
        sessions.register(deviceId, uuid, user.getId(), socket);
        deviceService.markOnline(deviceId);
        assertThat(deviceRepository.findById(deviceId).orElseThrow().getStatus()).isEqualTo(DeviceStatus.ONLINE);

        authService.logout(refreshToken);

        assertThat(deviceService.authenticate(deviceCredential)).isPresent();
        assertThat(sessions.isConnected(deviceId)).isTrue();
        assertThat(deviceRepository.findById(deviceId).orElseThrow().getStatus()).isEqualTo(DeviceStatus.ONLINE);

        DesktopDeviceEntity staleSeen = deviceRepository.findById(deviceId).orElseThrow();
        staleSeen.setLastSeen(Instant.now().minusSeconds(120));
        deviceRepository.save(staleSeen);
        deviceService.refreshConnectedPresence();
        deviceService.markStaleOffline(Instant.now().minusSeconds(45));
        assertThat(deviceRepository.findById(deviceId).orElseThrow().getStatus()).isEqualTo(DeviceStatus.ONLINE);
    }

    @Test
    @DisplayName("4b. A disconnected device past the heartbeat window is marked offline")
    void disconnectedDevicePastHeartbeatGoesOffline() {
        User user = createUser("stale-" + UUID.randomUUID() + "@tron.ai");
        String uuid = UUID.randomUUID().toString();
        var link = pairingService.createLinkRequest(uuid, "pub-key", "Windows 11", "PC", "0.1.0", "PC", "C:\\Dev");
        var authorized = pairingService.authorizeLinkRequest(user.getId(), (String) link.get("publicRequestId"), "C:\\Dev", "PC");
        Long deviceId = (Long) authorized.get("id");
        deviceService.markOnline(deviceId);
        DesktopDeviceEntity seen = deviceRepository.findById(deviceId).orElseThrow();
        seen.setLastSeen(Instant.now().minusSeconds(120));
        deviceRepository.saveAndFlush(seen);

        int changed = deviceService.markStaleOffline(Instant.now().minusSeconds(45));

        // Shared H2 may already contain other stale ONLINE rows from earlier cases.
        assertThat(changed).isGreaterThanOrEqualTo(1);
        assertThat(deviceRepository.findById(deviceId).orElseThrow().getStatus()).isEqualTo(DeviceStatus.OFFLINE);
    }

    @Test
    @DisplayName("5. Same owner repair rotates the credential and keeps the device row")
    void sameOwnerRepairRotatesCredential() {
        User user = createUser("repair-" + UUID.randomUUID() + "@tron.ai");
        String uuid = UUID.randomUUID().toString();
        var first = pairingService.createLinkRequest(uuid, "pub-key", "Windows 11", "HOST", "0.1.0", "PC", "C:\\Dev");
        var authorized = pairingService.authorizeLinkRequest(user.getId(), (String) first.get("publicRequestId"), "C:\\Dev", "PC");
        Long deviceId = (Long) authorized.get("id");
        var firstPoll = pairingService.poll((String) first.get("linkRequestId"), (String) first.get("pollSecret"));
        String oldCredential = (String) firstPoll.get("deviceCredential");

        var second = pairingService.createLinkRequest(uuid, "pub-key-2", "Windows 11", "HOST", "0.1.0", "PC", "C:\\Work");
        var repaired = pairingService.authorizeLinkRequest(user.getId(), (String) second.get("publicRequestId"), "C:\\Work", "PC");
        assertThat(repaired.get("id")).isEqualTo(deviceId);
        assertThat(repaired.get("repaired")).isEqualTo(true);
        assertThat(deviceRepository.findByDeviceUuid(uuid).orElseThrow().getId()).isEqualTo(deviceId);

        var secondPoll = pairingService.poll((String) second.get("linkRequestId"), (String) second.get("pollSecret"));
        String newCredential = (String) secondPoll.get("deviceCredential");
        assertThat(newCredential).isNotEqualTo(oldCredential);
        assertThat(deviceService.authenticate(oldCredential)).isEmpty();
        assertThat(deviceService.authenticate(newCredential)).isPresent();
        assertThat(pairingService.poll((String) second.get("linkRequestId"), (String) second.get("pollSecret")).get("deviceCredential")).isNull();
    }

    @Test
    @DisplayName("6. Pair-code fallback issues the same device credential model")
    void pairCodeFallbackUsesSameCredentialModel() {
        User user = createUser("pair-" + UUID.randomUUID() + "@tron.ai");
        String uuid = UUID.randomUUID().toString();
        var created = pairingService.createRequest(uuid, "pub-key", "Windows 11", "HOST", "0.1.0", "PC");
        pairingService.pair(user.getId(), (String) created.get("pairingCode"), "C:\\Dev", "PC");
        var poll = pairingService.poll((String) created.get("pairingRequestId"), (String) created.get("pollSecret"));
        assertThat(poll.get("deviceCredential")).isEqualTo(poll.get("credential"));
        assertThat(poll.get("approvalKey")).isNotNull();
        assertThat(poll.get("allowedWorkspace")).isEqualTo("C:\\Dev");
        assertThat(deviceService.authenticate((String) poll.get("deviceCredential"))).isPresent();
    }
}
