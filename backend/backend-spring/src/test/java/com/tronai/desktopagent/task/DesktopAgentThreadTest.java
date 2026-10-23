package com.tronai.desktopagent.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.auth.JwtService;
import com.tronai.desktopagent.device.DesktopDeviceEntity;
import com.tronai.desktopagent.device.DesktopDeviceRepository;
import com.tronai.desktopagent.device.DeviceStatus;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DesktopAgentThreadTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository users;
    @Autowired private DesktopDeviceRepository devices;
    @Autowired private AgentTaskRepository tasks;
    @Autowired private TaskMessages conversation;
    @Autowired private ObjectMapper objectMapper;

    private User owner;
    private String token;

    @BeforeEach
    void user() {
        owner = users.save(User.builder().email("thread-" + UUID.randomUUID() + "@tron.ai").displayName("Thread").passwordHash("x").role("USER").enabled(true).build());
        token = jwtService.generateAccessToken(owner.getId(), owner.getEmail(), "USER", "Thread");
    }

    @Test
    void messagesPersistAndWaitingUserIsVisible() throws Exception {
        DesktopDeviceEntity device = devices.save(DesktopDeviceEntity.builder()
                .userId(owner.getId())
                .deviceUuid(UUID.randomUUID().toString())
                .displayName("Thread-PC")
                .status(DeviceStatus.OFFLINE)
                .allowedWorkspace("C:/Dev")
                .build());
        AgentTaskEntity task = tasks.save(AgentTaskEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(owner.getId())
                .deviceId(device.getId())
                .goal("Inspect the workspace")
                .status(TaskStatus.COMPLETED)
                .controlMode(ControlMode.AGENT)
                .workspace("C:/Dev")
                .summary("Workspace inspected.")
                .actionCount(1)
                .needsReinspect(false)
                .build());
        conversation.append(task, MessageRole.USER, "password=hunter2 please continue", "ACCEPTED");
        conversation.append(task, MessageRole.AGENT, "Build succeeded. I found one warning.", "COMPLETED");

        String body = mockMvc.perform(get("/api/v1/agent-tasks/" + task.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("AGENT");
        assertThat(body).contains("Build succeeded. I found one warning.");
        assertThat(body).contains("[REDACTED]");
        assertThat(body).doesNotContain("hunter2");

        mockMvc.perform(post("/api/v1/agent-tasks/" + task.getId() + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "Now fix the warning too"))))
                .andExpect(status().isConflict());

        task.setStatus(TaskStatus.WAITING_USER);
        tasks.save(task);
        JsonNode waiting = objectMapper.readTree(mockMvc.perform(get("/api/v1/agent-tasks/" + task.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(waiting.path("status").asText()).isEqualTo("WAITING_USER");

        task.setStatus(TaskStatus.STOPPED);
        tasks.save(task);
        mockMvc.perform(post("/api/v1/agent-tasks/" + task.getId() + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "continue"))))
                .andExpect(status().isConflict());
    }
}
