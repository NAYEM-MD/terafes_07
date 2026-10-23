package com.tronai.desktopagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * Live multi-turn thread and visible-browser login handoff.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DesktopAgentThreadLiveTest {

    @LocalServerPort
    private int port;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository users;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void sameTaskContinuesAfterManualLogin() throws Exception {
        Path target = Path.of("..", "..", "desktop-agent", "target").toAbsolutePath().normalize();
        assumeTrue(Files.exists(target.resolve("classes/com/tronai/desktop/app/HeadlessMain.class")), "Desktop agent is not compiled");
        assumeTrue(Files.isDirectory(target.resolve("dependency")), "Desktop runtime dependencies are not copied");

        Path workspace = Files.createTempDirectory("tron-agent-thread");
        Path portFile = workspace.resolve("browser.port");
        Path identity = workspace.resolve("identity.json");
        String classpath = target.resolve("classes") + System.getProperty("path.separator") + target.resolve("dependency") + System.getProperty("file.separator") + "*";
        ProcessBuilder agentBuilder = new ProcessBuilder("java", "-cp", classpath, "com.tronai.desktop.app.HeadlessMain");
        agentBuilder.environment().put("TRON_CORE_URL", "ws://127.0.0.1:" + port + "/api/v1/desktop-agent/ws");
        agentBuilder.environment().put("TRON_WORKSPACE", workspace.toString());
        agentBuilder.environment().put("TRON_IDENTITY_FILE", identity.toString());
        agentBuilder.environment().put("TRON_BROWSER_PORT_FILE", portFile.toString());
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
        }, "agent-thread-log");
        reader.setDaemon(true);
        reader.start();

        HttpServer site = loginSite();
        site.start();
        StringBuilder events = new StringBuilder();
        AtomicBoolean open = new AtomicBoolean(true);
        try {
            String code = awaitLine(lines, "PAIRING_CODE=", Duration.ofSeconds(30)).substring("PAIRING_CODE=".length()).trim();
            User owner = users.save(User.builder()
                    .email("thread-live-" + UUID.randomUUID() + "@tron.ai")
                    .displayName("Thread")
                    .passwordHash("x")
                    .role("USER")
                    .enabled(true)
                    .build());
            String token = jwtService.generateAccessToken(owner.getId(), owner.getEmail(), "USER", "Thread");
            MvcResult paired = mockMvc.perform(post("/api/v1/desktop-agents/pair")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "code", code,
                                    "allowedWorkspace", workspace.toString(),
                                    "displayName", "Thread-PC"))))
                    .andExpect(status().isOk())
                    .andReturn();
            long deviceId = objectMapper.readTree(paired.getResponse().getContentAsString()).path("id").asLong();
            awaitLine(lines, "STATUS=Connected", Duration.ofSeconds(30));

            String loginUrl = "http://127.0.0.1:" + site.getAddress().getPort() + "/login";
            JsonNode created = startTask(token, deviceId, workspace, "Open the test site and verify the account page. " + loginUrl);
            String taskId = created.path("id").asText();
            openSse(token, "/api/v1/agent-tasks/" + taskId + "/events", events, open);
            JsonNode waiting = awaitStatus(token, taskId, "WAITING_USER", Duration.ofSeconds(90));
            assertThat(waiting.path("summary").asText()).contains("Please log in manually");
            assertThat(waiting.path("browser").path("state").asText()).isEqualTo("WAITING_USER");
            assertThat(waiting.path("messages").toString()).contains("USER");
            assertThat(waiting.toString()).doesNotContain("tron-pass");
            awaitText(events, "AGENT_MESSAGE_STARTED", Duration.ofSeconds(10));
            awaitText(events, "AGENT_MESSAGE_DELTA", Duration.ofSeconds(10));
            awaitText(events, "AGENT_MESSAGE_COMPLETED", Duration.ofSeconds(10));
            awaitText(events, "WAITING_USER", Duration.ofSeconds(10));

            int helperPort = Integer.parseInt(Files.readString(portFile).trim());
            human(helperPort, "/human/type", Map.of("selector", "#email", "text", "tron"));
            human(helperPort, "/human/type", Map.of("selector", "#password", "text", "tron-pass"));
            human(helperPort, "/human/click", Map.of("selector", "#enter"));

            mockMvc.perform(post("/api/v1/agent-tasks/" + taskId + "/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", "continue"))))
                    .andExpect(status().isOk());
            JsonNode done = awaitFinished(token, taskId, Duration.ofSeconds(60));
            assertThat(done.path("status").asText()).as(done.path("summary").asText()).isIn("COMPLETED", "VERIFIED");
            assertThat(done.path("id").asText()).isEqualTo(taskId);
            assertThat(done.path("executions").toString()).contains("TRON Logged In");
            awaitText(events, "iVBORw0KGgo", Duration.ofSeconds(15));
            awaitText(events, "screenshotBase64", Duration.ofSeconds(5));

            mockMvc.perform(post("/api/v1/agent-tasks/" + taskId + "/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", "Now open the settings page."))))
                    .andExpect(status().isOk());
            JsonNode settings = awaitFinished(token, taskId, Duration.ofSeconds(60));
            assertThat(settings.path("status").asText()).as(settings.path("summary").asText()).isIn("COMPLETED", "VERIFIED");
            assertThat(settings.path("id").asText()).isEqualTo(taskId);
            assertThat(settings.path("executions").toString()).contains("TRON Settings");
            assertThat(settings.path("messages").toString()).contains("Now open the settings page.");

            mockMvc.perform(post("/api/v1/agent-tasks/" + taskId + "/stop")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            Thread.sleep(1000);
            boolean closed = false;
            try {
                human(helperPort, "/inspect", Map.of());
            } catch (Exception e) {
                closed = true;
            }
            assertThat(closed).isTrue();
        } finally {
            open.set(false);
            site.stop(0);
            agent.destroyForcibly();
        }
    }

    private HttpServer loginSite() throws Exception {
        HttpServer site = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        site.createContext("/login", exchange -> html(exchange, """
                <html><head><title>TRON Login</title></head><body>
                <p>TRON Login Required</p>
                <input id="email"><input id="password" type="password">
                <button id="enter" type="button">Sign in</button>
                <script>
                document.getElementById('enter').onclick = () => {
                  const email = document.getElementById('email').value;
                  const password = document.getElementById('password').value;
                  if (email === 'tron' && password === 'tron-pass') location.href = '/account';
                };
                </script></body></html>
                """));
        site.createContext("/account", exchange -> html(exchange, """
                <html><head><title>TRON Account</title></head><body><p>TRON Logged In</p></body></html>
                """));
        site.createContext("/settings", exchange -> html(exchange, """
                <html><head><title>TRON Settings</title></head><body><p>TRON Settings</p></body></html>
                """));
        return site;
    }

    private void human(int helperPort, String path, Map<String, Object> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + helperPort + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Human browser action failed: " + response.statusCode());
        }
    }

    private static void html(HttpExchange exchange, String html) throws java.io.IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private void openSse(String token, String path, StringBuilder sink, AtomicBoolean open) throws Exception {
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
                // Closed with the test.
            }
        }, "sse-thread");
        thread.setDaemon(true);
        thread.start();
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
        throw new AssertionError("Did not see " + expected);
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
            Thread.sleep(200);
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
