package com.tronai.agentengine;

import com.tronai.auth.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent-engines")
public class AgentEngineController {

    private final AgentEngineRegistry registry;

    public AgentEngineController(AgentEngineRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(registry.healthAll());
    }

    @PostMapping("/{engine}/sessions")
    public ResponseEntity<Map<String, Object>> create(
            @PathVariable String engine,
            @RequestBody Map<String, String> body) {
        AgentEngine eng = registry.require(AgentEngineType.valueOf(engine.trim().toUpperCase()));
        Long deviceId = null;
        if (body.get("deviceId") != null && !body.get("deviceId").isBlank()) {
            deviceId = Long.parseLong(body.get("deviceId").trim());
        }
        return ResponseEntity.ok(eng.createSession(
                SecurityUtils.getRequiredUserId(),
                body.get("goal"),
                body.get("workspacePath"),
                body.get("planId"),
                deviceId));
    }

    @PostMapping("/{engine}/sessions/{id}/verify-file")
    public ResponseEntity<Map<String, Object>> verifyFile(
            @PathVariable String engine,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        AgentEngine eng = registry.require(AgentEngineType.valueOf(engine.trim().toUpperCase()));
        if (!(eng instanceof OpenCodeAgentEngine openCode)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "detail", "verify-file is only supported for OPENCODE"));
        }
        return ResponseEntity.ok(openCode.verifyFile(
                SecurityUtils.getRequiredUserId(), id, body.get("path")));
    }

    @PostMapping("/{engine}/sessions/{id}/start")
    public ResponseEntity<Map<String, Object>> start(
            @PathVariable String engine,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        AgentEngine eng = registry.require(AgentEngineType.valueOf(engine.trim().toUpperCase()));
        return ResponseEntity.ok(eng.startTask(SecurityUtils.getRequiredUserId(), id, body.get("prompt")));
    }

    @PostMapping("/{engine}/sessions/{id}/continue")
    public ResponseEntity<Map<String, Object>> cont(
            @PathVariable String engine,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        AgentEngine eng = registry.require(AgentEngineType.valueOf(engine.trim().toUpperCase()));
        return ResponseEntity.ok(eng.continueSession(SecurityUtils.getRequiredUserId(), id, body.get("prompt")));
    }

    @GetMapping("/{engine}/sessions/{id}")
    public ResponseEntity<Map<String, Object>> status(
            @PathVariable String engine,
            @PathVariable String id) {
        AgentEngine eng = registry.require(AgentEngineType.valueOf(engine.trim().toUpperCase()));
        return ResponseEntity.ok(eng.status(SecurityUtils.getRequiredUserId(), id));
    }

    @PostMapping("/{engine}/sessions/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable String engine,
            @PathVariable String id) {
        AgentEngine eng = registry.require(AgentEngineType.valueOf(engine.trim().toUpperCase()));
        return ResponseEntity.ok(eng.cancel(SecurityUtils.getRequiredUserId(), id));
    }
}
