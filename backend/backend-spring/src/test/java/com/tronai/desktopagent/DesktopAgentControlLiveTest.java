package com.tronai.desktopagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
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
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Live pause, stop, take-control, Playwright, and SSE checks against a real desktop agent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DesktopAgentControlLiveTest {

    @LocalServerPort
    private int port;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository users;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void pauseStopTakeControlBrowserAndSse() throws Exception {
        Path target = Path.of("..", "..", "desktop-agent", "target").toAbsolutePath().normalize();
        assumeTrue(Files.exists(target.resolve("classes/com/tronai/desktop/app/HeadlessMain.class")), "Desktop agent is not compiled");
        assumeTrue(Files.isDirectory(target.resolve("dependency")), "Desktop runtime dependencies are not copied");

        Path workspace = Files.createTempDirectory("tron-agent-control");
        new ProcessBuilder("git", "init").directory(workspace.toFile()).start().waitFor();
        Path identity = workspace.resolve("identity.json");
        String classpath = target.resolve("classes") + System.getProperty("path.separator") + target.resolve("dependency") + System.getProperty("file.separator") + "*";
        ProcessBuilder agentBuilder = new ProcessBuilder("java", "-cp", classpath, "com.tronai.desktop.app.HeadlessMain");
        agentBuilder.environment().put("TRON_CORE_URL", "ws://127.0.0.1:" + port + "/api/v1/desktop-agent/ws");
        agentBuilder.environment().put("TRON_WORKSPACE", workspace.toString());
        agentBuilder.environment().put("TRON_IDENTITY_FILE", identity.toString());
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
        }, "agent-control-log");
        reader.setDaemon(true);
        reader.start();

        HttpServer page = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        page.createContext("/tron", exchange -> {
            byte[] body = "<html><head><title>TRON Browser</title></head><body><p id=\"msg\">TRON Desktop Agent works</p></body></html>"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        page.start();

        StringBuilder taskEvents = new StringBuilder();
        StringBuilder deviceEvents = new StringBuilder();
        AtomicBoolean open = new AtomicBoolean(true);
        List<HttpURLConnection> streams = new ArrayList<>();
        try {
            String code = awaitLine(lines, "PAIRING_CODE=", Duration.ofSeconds(30)).substring("PAIRING_CODE=".length()).trim();
            User owner = users.save(User.builder()
                    .email("control-" + UUID.randomUUID() + "@tron.ai")
                    .displayName("Control")
                    .passwordHash("x")
                    .role("USER")
                    .enabled(true)
                    .build());
            String token = jwtService.generateAccessToken(owner.getId(), owner.getEmail(), "USER", "Control");
            MvcResult paired = mockMvc.perform(post("/api/v1/desktop-agents/pair")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "code", code,
                                    "allowedWorkspace", workspace.toString(),
                                    "displayName", "Control-PC"))))
                    .andExpect(status().isOk())
                    .andReturn();
            long deviceId = objectMapper.readTree(paired.getResponse().getContentAsString()).path("id").asLong();
            awaitLine(lines, "STATUS=Connected", Duration.ofSeconds(30));
            streams.add(openSse(token, "/api/v1/desktop-agents/events", deviceEvents, open));
            awaitText(deviceEvents, "connected", Duration.ofSeconds(10));
            phase("device-sse-open");

            JsonNode pausedTask = startTask(token, deviceId, workspace, "Run a long ping then git status.");
            streams.add(openSse(token, "/api/v1/agent-tasks/" + pausedTask.path("id").asText() + "/events", taskEvents, open));
            awaitText(taskEvents, "connected", Duration.ofSeconds(10));
            awaitStatus(token, pausedTask.path("id").asText(), "RUNNING", Duration.ofSeconds(20));
            mockMvc.perform(post("/api/v1/agent-tasks/" + pausedTask.path("id").asText() + "/pause")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            phase("pause-accepted");
            Thread.sleep(16000);
            phase("paused-checking");
            JsonNode held = fetch(token, pausedTask.path("id").asText());
            assertThat(held.path("status").asText()).isEqualTo("PAUSED");
            assertThat(stepStatus(held, "Running git status")).isEmpty();
            mockMvc.perform(post("/api/v1/agent-tasks/" + pausedTask.path("id").asText() + "/resume")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            JsonNode continued = awaitFinished(token, pausedTask.path("id").asText(), Duration.ofSeconds(40));
            assertThat(continued.path("status").asText()).isIn("COMPLETED", "VERIFIED");
            assertThat(continued.path("controlMode").asText()).isEqualTo("AGENT");
            phase("pause-resumed");
            assertThat(stepStatus(continued, "Running git status")).as(continued.path("steps").toString()).isEqualTo("COMPLETED");

            JsonNode stopTask = startTask(token, deviceId, workspace, "Run a long ping then git status.");
            awaitStatus(token, stopTask.path("id").asText(), "RUNNING", Duration.ofSeconds(20));
            awaitPing(agent, Duration.ofSeconds(10));
            mockMvc.perform(post("/api/v1/agent-tasks/" + stopTask.path("id").asText() + "/stop")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            JsonNode stopped = awaitStatus(token, stopTask.path("id").asText(), "STOPPED", Duration.ofSeconds(15));
            awaitNoPing(agent, Duration.ofSeconds(10));
            stopped = fetch(token, stopTask.path("id").asText());
            assertThat(stopped.path("status").asText()).isEqualTo("STOPPED");
            assertThat(stopped.path("summary").asText()).isEqualTo("Task stopped");
            assertThat(stepStatus(stopped, "Running long ping")).isEqualTo("FAILED");
            assertThat(stopped.toString()).doesNotContain("Sent = 12");
            assertThat(stepStatus(stopped, "Running git status")).isEmpty();

            JsonNode manualTask = startTask(token, deviceId, workspace, "Run a long ping then git status.");
            awaitStatus(token, manualTask.path("id").asText(), "RUNNING", Duration.ofSeconds(20));
            JsonNode taken = objectMapper.readTree(mockMvc.perform(post("/api/v1/agent-tasks/" + manualTask.path("id").asText() + "/take-control")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());
            assertThat(taken.path("status").asText()).isEqualTo("PAUSED");
            assertThat(taken.path("controlMode").asText()).isEqualTo("MANUAL");
            phase("manual-holding");
            Thread.sleep(16000);
            phase("manual-checking");
            JsonNode stillManual = fetch(token, manualTask.path("id").asText());
            assertThat(stillManual.path("status").asText()).isEqualTo("PAUSED");
            assertThat(stillManual.path("controlMode").asText()).isEqualTo("MANUAL");
            assertThat(stepStatus(stillManual, "Running git status")).isEmpty();
            mockMvc.perform(post("/api/v1/agent-tasks/" + manualTask.path("id").asText() + "/resume")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            JsonNode resumed = awaitFinished(token, manualTask.path("id").asText(), Duration.ofSeconds(40));
            assertThat(resumed.path("status").asText()).isIn("COMPLETED", "VERIFIED");
            assertThat(resumed.path("controlMode").asText()).isEqualTo("AGENT");
            assertThat(stepStatus(resumed, "Running git status")).isEqualTo("COMPLETED");

            phase("browser-starting");
            String url = "http://127.0.0.1:" + page.getAddress().getPort() + "/tron";
            JsonNode browser = runTask(token, deviceId, workspace, "Use the browser to open " + url);
            assertThat(browser.path("status").asText()).as(browser.path("summary").asText()).isIn("COMPLETED", "VERIFIED");
            assertThat(browser.path("executions").toString()).contains("TRON Desktop Agent works");
            assertThat(browser.path("executions").toString()).contains("iVBORw0KGgo");
            awaitText(deviceEvents, "TRON Browser", Duration.ofSeconds(15));
            String live = taskEvents + deviceEvents.toString();
            assertThat(live).contains("TASK_STATUS");
            assertThat(live).contains("STEP_STARTED");
            assertThat(live).contains("TERMINAL_OUTPUT");
            assertThat(live).contains("STEP_COMPLETED");
            assertThat(live).contains("TASK_COMPLETED");
            assertThat(live).contains("BROWSER_EVENT");
            assertThat(live).contains("DEVICE_STATUS");
        } catch (Throwable failure) {
            phase("failed " + failure.getClass().getSimpleName() + " " + failure.getMessage());
            throw failure;
        } finally {
            open.set(false);
            page.stop(0);
            agent.destroyForcibly();
        }
    }

    private HttpURLConnection openSse(String token, String path, StringBuilder sink, AtomicBoolean open) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + path).toURL().openConnection();
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(0);
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (open.get() && (line = reader.readLine()) != null) {
                    synchronized (sink) {
                        sink.append(line).append('\n');
                    }
                }
            } catch (Exception ignored) {
                // The stream is closed when the test finishes.
            }
        }, "sse-" + path);
        thread.setDaemon(true);
        thread.start();
        return connection;
    }

    private static void phase(String name) {
        try {
            java.nio.file.Files.writeString(Path.of("target", "live-phase.txt"),
                    java.time.Instant.now() + " " + name + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Phase notes are diagnostic only.
        }
    }

    private static void awaitPing(Process agent, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (commands(agent).stream().anyMatch(command -> command.toLowerCase().contains("ping"))) {
                return;
            }
            Thread.sleep(150);
        }
        throw new AssertionError("Owned ping process did not start: " + commands(agent));
    }

    private static void awaitNoPing(Process agent, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<String> last = List.of();
        while (System.nanoTime() < deadline) {
            last = commands(agent);
            if (last.stream().noneMatch(command -> command.toLowerCase().contains("ping"))) {
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Owned ping process was still running: " + last);
    }

    private static List<String> commands(Process agent) {
        return agent.descendants()
                .map(ProcessHandle::info)
                .map(info -> info.command().orElse("") + " " + String.join(" ", info.arguments().orElse(new String[0])))
                .toList();
    }

    private static void awaitText(StringBuilder sink, String expected, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            synchronized (sink) {
                if (sink.indexOf(expected) >= 0) {
                    return;
                }
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Did not see " + expected + " in " + sink);
    }

    private static String stepStatus(JsonNode task, String summary) {
        for (JsonNode step : task.path("steps")) {
            if (summary.equals(step.path("summary").asText())) {
                return step.path("status").asText();
            }
        }
        return "";
    }

    private JsonNode runTask(String token, long deviceId, Path workspace, String goal) throws Exception {
        JsonNode created = startTask(token, deviceId, workspace, goal);
        return awaitFinished(token, created.path("id").asText(), Duration.ofSeconds(90));
    }

    private JsonNode startTask(String token, long deviceId, Path workspace, String goal) throws Exception {
        MvcResult result = null;
        for (int attempt = 0; attempt < 30; attempt++) {
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

    private JsonNode awaitStatus(String token, String taskId, String expected, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        JsonNode last = null;
        while (System.nanoTime() < deadline) {
            last = fetch(token, taskId);
            if (expected.equals(last.path("status").asText())) {
                return last;
            }
            if (List.of("COMPLETED", "VERIFIED", "FAILED", "STOPPED").contains(last.path("status").asText()) && !expected.equals(last.path("status").asText())) {
                break;
            }
            Thread.sleep(150);
        }
        throw new AssertionError("Expected " + expected + " but last payload was " + last);
    }

    private JsonNode awaitFinished(String token, String taskId, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        JsonNode last = null;
        while (System.nanoTime() < deadline) {
            last = fetch(token, taskId);
            if (List.of("COMPLETED", "VERIFIED", "FAILED", "STOPPED").contains(last.path("status").asText())) {
                return last;
            }
            Thread.sleep(200);
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
