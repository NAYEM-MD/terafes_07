package com.tronai.supervisor;

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
@RequestMapping("/api/v1/supervisor")
public class SupervisorController {

    private final SupervisorService supervisor;

    public SupervisorController(SupervisorService supervisor) {
        this.supervisor = supervisor;
    }

    @PostMapping("/plans")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getRequiredUserId();
        return ResponseEntity.ok(supervisor.createPlan(
                userId,
                body.get("goal"),
                body.get("engine")));
    }

    @GetMapping("/plans")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(supervisor.list(SecurityUtils.getRequiredUserId()));
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        return ResponseEntity.ok(supervisor.view(id, SecurityUtils.getRequiredUserId()));
    }

    @PostMapping("/plans/{id}/milestones/{index}/result")
    public ResponseEntity<Map<String, Object>> milestoneResult(
            @PathVariable String id,
            @PathVariable int index,
            @RequestBody Map<String, Object> body) {
        boolean success = Boolean.TRUE.equals(body.get("success"));
        boolean verified = Boolean.TRUE.equals(body.get("verified"));
        String evidence = body.get("evidence") == null ? "" : String.valueOf(body.get("evidence"));
        return ResponseEntity.ok(supervisor.markMilestoneResult(
                SecurityUtils.getRequiredUserId(), id, index, success, evidence, verified));
    }
}
