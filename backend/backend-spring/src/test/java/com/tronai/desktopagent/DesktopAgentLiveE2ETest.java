package com.tronai.desktopagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.auth.JwtService;
import com.tronai.user.User;
import com.tronai.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real two-process test: Spring on a random port and the desktop agent runtime.
 * Skips when the desktop agent has not been compiled yet.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DesktopAgentLiveE2ETest {

    @LocalServerPort
    private int port;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository users;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void pairsAndRunsRealGitStatusFileEditAndApproval() throws Exception {
        Path classes = Path.of("..", "..", "desktop-agent", "target", "classes",
                "com", "tronai", "desktop", "app", "HeadlessMain.class").toAbsolutePath().normalize();
        assumeTrue(Files.exists(classes), "Desktop agent is not compiled");
        Path dependency = classes.getParent().getParent().getParent().getParent().getParent().resolve("dependency");
        // classes = desktop-agent/target/classes/com/tronai/desktop/app/HeadlessMain.class
        Path target = Path.of("..", "..", "desktop-agent", "target").toAbsolutePath().normalize();
        assumeTrue(Files.isDirectory(target.resolve("dependency")), "Desktop runtime dependencies are not copied");

        Path workspace = Files.createTempDirectory("tron-agent-e2e");
        new ProcessBuilder("git", "init").directory(workspace.toFile()).start().waitFor();
        Path identity = workspace.resolve("identity.json");
        String classpath = target.resolve("classes") + System.getProperty("path.separator") + target.resolve("dependency") + System.getProperty("file.separator") + "*";
        ProcessBuilder agentBuilder = new ProcessBuilder("java", "-cp", classpath, "com.tronai.desktop.app.HeadlessMain");
        agentBuilder.environment().put("TRON_CORE_URL", "ws://127.0.0.1:" + port + "/api/v1/desktop-agent/ws");
        agentBuilder.environment().put("TRON_WORKSPACE", workspace.toString());
        agentBuilder.environment().put("TRON_IDENTITY_FILE", identity.toString());
        // Ensure Windows agent can resolve mvn.cmd the same way this JVM can.
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null && !pathEnv.isBlank()) {
            agentBuilder.environment().put("PATH", pathEnv);
        }
        agentBuilder.redirectErrorStream(true);
        Process agent = agentBuilder.start();
        BlockingQueue<String> lines = new LinkedBlockingQueue<>();
        Thread reader = new Thread(() -> {
            try (BufferedReader buffered = new BufferedReader(new InputStreamReader(agent.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = buffered.readLine()) != null) {
                    lines.add(line);
                }
            } catch (Exception ignored) {
                lines.add("AGENT_STREAM_CLOSED");
            }
        }, "agent-e2e-log");
        reader.setDaemon(true);
        reader.start();
        try {
            String code = awaitLine(lines, "PAIRING_CODE=", Duration.ofSeconds(30)).substring("PAIRING_CODE=".length()).trim();
            User owner = users.save(User.builder()
                    .email("e2e-" + UUID.randomUUID() + "@tron.ai")
                    .displayName("E2E")
                    .passwordHash("x")
                    .role("USER")
                    .enabled(true)
                    .build());
            String token = jwtService.generateAccessToken(owner.getId(), owner.getEmail(), "USER", "E2E");
            MvcResult paired = mockMvc.perform(post("/api/v1/desktop-agents/pair")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "code", code,
                                    "allowedWorkspace", workspace.toString(),
                                    "displayName", "E2E-PC"))))
                    .andExpect(status().isOk())
                    .andReturn();
            long deviceId = objectMapper.readTree(paired.getResponse().getContentAsString()).path("id").asLong();
            awaitLine(lines, "STATUS=Connected", Duration.ofSeconds(30));

            JsonNode inspected = runTask(token, deviceId, workspace, "Inspect the selected workspace and run git status.");
            assertThat(inspected.path("status").asText())
                    .withFailMessage(inspected.toPrettyString() + "\nagent=" + lines)
                    .isIn("COMPLETED", "VERIFIED");
            String blob = inspected.toString().toLowerCase();
            assertThat(blob).contains("git");
            assertThat(blob).contains("branch");

            runTask(token, deviceId, workspace, "Create hello.txt containing TRON Desktop Agent works");
            assertThat(Files.readString(workspace.resolve("hello.txt"))).contains("TRON Desktop Agent works");
            runTask(token, deviceId, workspace, "Edit hello.txt and change the text");
            assertThat(Files.readString(workspace.resolve("hello.txt"))).contains("Edited by TRON");

            JsonNode waiting = startTask(token, deviceId, workspace, "Delete hello.txt");
            JsonNode deniedState = awaitStatus(token, waiting.path("id").asText(), "WAITING_APPROVAL", Duration.ofSeconds(40));
            assertThat(Files.exists(workspace.resolve("hello.txt"))).isTrue();
            String approvalId = deniedState.path("approvals").get(0).path("id").asText();
            mockMvc.perform(post("/api/v1/agent-tasks/" + waiting.path("id").asText() + "/approvals/" + approvalId + "/deny")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            awaitFinished(token, waiting.path("id").asText(), Duration.ofSeconds(40));
            assertThat(Files.exists(workspace.resolve("hello.txt"))).isTrue();

            JsonNode second = startTask(token, deviceId, workspace, "Delete hello.txt");
            JsonNode approvalState = awaitStatus(token, second.path("id").asText(), "WAITING_APPROVAL", Duration.ofSeconds(40));
            String secondApproval = approvalState.path("approvals").get(0).path("id").asText();
            mockMvc.perform(post("/api/v1/agent-tasks/" + second.path("id").asText() + "/approvals/" + secondApproval + "/approve")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            JsonNode deleted = awaitFinished(token, second.path("id").asText(), Duration.ofSeconds(40));
            assertThat(deleted.path("status").asText()).isIn("COMPLETED", "VERIFIED");
            assertThat(Files.exists(workspace.resolve("hello.txt"))).isFalse();

            JsonNode push = startTask(token, deviceId, workspace, "Run git push origin main");
            JsonNode pushState = awaitStatus(token, push.path("id").asText(), "WAITING_APPROVAL", Duration.ofSeconds(40));
            String pushApproval = pushState.path("approvals").get(0).path("id").asText();
            mockMvc.perform(post("/api/v1/agent-tasks/" + push.path("id").asText() + "/approvals/" + pushApproval + "/deny")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            JsonNode pushDone = awaitFinished(token, push.path("id").asText(), Duration.ofSeconds(40));
            assertThat(pushDone.toString().toLowerCase()).contains("denied");

            // Prefer a goal that creates a known-good sample project (deterministic MAVEN_PROJECT mission)
            // rather than relying on a host-global `mvn` against a stub pom.
            assumeTrue(isCommandAvailable("mvn"), "mvn is not available on PATH for Desktop Agent E2E");
            JsonNode mavenCreated = startTask(token, deviceId, workspace,
                    "Create a simple Maven project with unit tests and run them");
            JsonNode maven = awaitFinished(token, mavenCreated.path("id").asText(), Duration.ofSeconds(180));
            assertThat(maven.path("status").asText()).as(maven.path("summary").asText()).isIn("COMPLETED", "VERIFIED");
            assertThat(maven.toString().toLowerCase()).contains("maven");
        } finally {
            agent.destroyForcibly();
        }
    }

    private static boolean isCommandAvailable(String command) {
        // Windows ProcessBuilder does not apply PATHEXT the same way as cmd.exe —
        // try .cmd/.bat explicitly so Maven on PATH is detected.
        List<String> candidates = new ArrayList<>();
        candidates.add(command);
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            candidates.add(command + ".cmd");
            candidates.add(command + ".bat");
        }
        for (String candidate : candidates) {
            try {
                Process process = new ProcessBuilder(candidate, "-version")
                        .redirectErrorStream(true)
                        .start();
                boolean finished = process.waitFor(15, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    return true;
                }
            } catch (Exception ignored) {
                // try next candidate
            }
        }
        return false;
    }

    private JsonNode runTask(String token, long deviceId, Path workspace, String goal) throws Exception {
        JsonNode created = startTask(token, deviceId, workspace, goal);
        return awaitFinished(token, created.path("id").asText(), Duration.ofSeconds(60));
    }

    private JsonNode startTask(String token, long deviceId, Path workspace, String goal) throws Exception {
        MvcResult result = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            result = mockMvc.perform(post("/api/v1/agent-tasks")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "deviceId", deviceId,
                                    "goal", goal,
                                    "workspace", workspace.toString()))))
                    .andReturn();
            if (result.getResponse().getStatus() == 201) {
                return objectMapper.readTree(result.getResponse().getContentAsString());
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Task was not accepted: " + (result == null ? "" : result.getResponse().getContentAsString()));
    }

    private JsonNode awaitStatus(String token, String taskId, String status, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        JsonNode last = null;
        while (System.nanoTime() < deadline) {
            last = fetch(token, taskId);
            if (status.equals(last.path("status").asText())) {
                return last;
            }
            if (List.of("COMPLETED", "VERIFIED", "FAILED", "STOPPED").contains(last.path("status").asText())) {
                break;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Expected " + status + " but last payload was " + last);
    }

    private JsonNode awaitFinished(String token, String taskId, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        JsonNode last = null;
        while (System.nanoTime() < deadline) {
            last = fetch(token, taskId);
            String status = last.path("status").asText();
            if (List.of("COMPLETED", "VERIFIED", "FAILED", "STOPPED").contains(status)) {
                return last;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Task did not finish: " + last);
    }

    private JsonNode fetch(String token, String taskId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/agent-tasks/" + taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String awaitLine(BlockingQueue<String> lines, String prefix, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<String> seen = new ArrayList<>();
        while (System.nanoTime() < deadline) {
            String line = lines.poll(500, TimeUnit.MILLISECONDS);
            if (line != null) {
                seen.add(line);
                if (line.startsWith(prefix)) {
                    return line;
                }
            }
        }
        throw new AssertionError("Did not see " + prefix + " in " + seen);
    }
}
