package com.tronai.desktopagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.auth.JwtService;
import com.tronai.desktopagent.approval.AgentApprovalEntity;
import com.tronai.desktopagent.approval.ApprovalService;
import com.tronai.desktopagent.device.DesktopDeviceEntity;
import com.tronai.desktopagent.device.DesktopDeviceRepository;
import com.tronai.desktopagent.device.DeviceService;
import com.tronai.desktopagent.device.DeviceStatus;
import com.tronai.desktopagent.policy.Fingerprints;
import com.tronai.desktopagent.policy.RiskLevel;
import com.tronai.desktopagent.task.AgentTaskEntity;
import com.tronai.desktopagent.task.AgentTaskRepository;
import com.tronai.desktopagent.task.ControlMode;
import com.tronai.desktopagent.task.TaskStatus;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DesktopAgentPlatformTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository users;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DeviceService deviceService;
    @Autowired private DesktopDeviceRepository devices;
    @Autowired private AgentTaskRepository tasks;
    @Autowired private ApprovalService approvals;

    private User owner;
    private User other;
    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void users() {
        owner = users.save(User.builder().email("owner-" + UUID.randomUUID() + "@tron.ai").displayName("Owner").passwordHash("x").role("USER").enabled(true).build());
        other = users.save(User.builder().email("other-" + UUID.randomUUID() + "@tron.ai").displayName("Other").passwordHash("x").role("USER").enabled(true).build());
        ownerToken = jwtService.generateAccessToken(owner.getId(), owner.getEmail(), "USER", "Owner");
        otherToken = jwtService.generateAccessToken(other.getId(), other.getEmail(), "USER", "Other");
    }

    @Test
    void pairingIsSingleUseAndOwnedByTheAuthenticatedUser() throws Exception {
        String deviceUuid = UUID.randomUUID().toString();
        MvcResult created = mockMvc.perform(post("/api/v1/desktop-agents/pairing-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deviceUuid", deviceUuid,
                                "publicKey", "test-public-key",
                                "hostname", "test-pc",
                                "os", "Windows",
                                "agentVersion", "0.1.0"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String code = body.path("pairingCode").asText();
        String requestId = body.path("pairingRequestId").asText();
        String pollSecret = body.path("pollSecret").asText();
        assertThat(code).matches("\\d{3}-\\d{3}");

        mockMvc.perform(post("/api/v1/desktop-agents/pair")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code, "allowedWorkspace", "C:/Dev"))))
                .andExpect(status().isUnauthorized());

        MvcResult paired = mockMvc.perform(post("/api/v1/desktop-agents/pair")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code, "allowedWorkspace", "C:/Dev", "displayName", "Thushan-PC"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode device = objectMapper.readTree(paired.getResponse().getContentAsString());
        assertThat(device.path("credential").isMissingNode() || device.path("credential").isNull()).isTrue();
        assertThat(device.path("displayName").asText()).isEqualTo("Thushan-PC");

        mockMvc.perform(post("/api/v1/desktop-agents/pair")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code, "allowedWorkspace", "C:/Dev"))))
                .andExpect(status().isConflict());

        MvcResult polled = mockMvc.perform(get("/api/v1/desktop-agents/pairing-requests/" + requestId)
                        .header("X-Tron-Poll-Secret", pollSecret))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode poll = objectMapper.readTree(polled.getResponse().getContentAsString());
        assertThat(poll.path("status").asText()).isEqualTo("PAIRED");
        assertThat(poll.path("credential").asText()).isNotBlank();
        String credential = poll.path("credential").asText();

        mockMvc.perform(get("/api/v1/desktop-agents/pairing-requests/" + requestId)
                        .header("X-Tron-Poll-Secret", pollSecret))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).doesNotContain(credential));

        mockMvc.perform(get("/api/v1/desktop-agents/devices").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains("Thushan-PC"));
        mockMvc.perform(get("/api/v1/desktop-agents/devices").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).doesNotContain("Thushan-PC"));

        assertThat(deviceService.authenticate(credential)).isPresent();
        Long deviceId = device.path("id").asLong();
        mockMvc.perform(delete("/api/v1/desktop-agents/devices/" + deviceId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/desktop-agents/devices/" + deviceId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
        assertThat(deviceService.authenticate(credential)).isEmpty();
        DesktopDeviceEntity revoked = devices.findById(deviceId).orElseThrow();
        assertThat(revoked.getStatus()).isEqualTo(DeviceStatus.REVOKED);

        mockMvc.perform(post("/api/v1/agent-tasks")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deviceId", deviceId, "goal", "Inspect"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void approvalIsBoundToTheFingerprintAndTheOwner() {
        DesktopDeviceEntity device = devices.save(DesktopDeviceEntity.builder()
                .userId(owner.getId())
                .deviceUuid(UUID.randomUUID().toString())
                .displayName("pc")
                .credentialHash(Fingerprints.sha256("cred"))
                .status(DeviceStatus.ONLINE)
                .allowedWorkspace("C:/Dev")
                .build());
        AgentTaskEntity task = tasks.save(AgentTaskEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(owner.getId())
                .deviceId(device.getId())
                .goal("delete hello")
                .status(TaskStatus.RUNNING)
                .controlMode(ControlMode.AGENT)
                .workspace("C:/Dev")
                .actionCount(0)
                .build());
        String actionId = UUID.randomUUID().toString();
        String fingerprint = Fingerprints.sha256("delete-hello");
        AgentApprovalEntity approval = approvals.createPending(task, actionId, fingerprint, RiskLevel.REQUIRES_APPROVAL,
                "Deleting hello.txt", "delete hello.txt", "C:/Dev/hello.txt");
        assertThatThrownBy(() -> approvals.decide(other.getId(), task.getId(), approval.getId(), true))
                .isInstanceOf(DesktopAgentException.class);
        approvals.decide(owner.getId(), task.getId(), approval.getId(), true);
        assertThatThrownBy(() -> approvals.consume(approval.getId(), "different-fingerprint"))
                .isInstanceOf(DesktopAgentException.class);
        assertThat(approvals.consume(approval.getId(), fingerprint).getStatus().name()).isEqualTo("CONSUMED");
    }
}
