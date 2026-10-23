package com.tronai.admin;

import com.tronai.auth.SecurityUtils;
import com.tronai.hybrid.PlatformControlEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class AdminController {

    private final AdminControlService admin;

    public AdminController(AdminControlService admin) {
        this.admin = admin;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(admin.dashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<?> users() {
        return ResponseEntity.ok(admin.listUsers());
    }

    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ResponseEntity.ok(admin.suspendUser(SecurityUtils.getRequiredUserId(), id, reason));
    }

    @PostMapping("/users/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        return ResponseEntity.ok(admin.restoreUser(SecurityUtils.getRequiredUserId(), id));
    }

    @GetMapping("/control")
    public ResponseEntity<?> control() {
        return ResponseEntity.ok(admin.dashboard().get("hybrid"));
    }

    @PutMapping("/control")
    public ResponseEntity<PlatformControlEntity> updateControl(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(admin.applyControlPatch(SecurityUtils.getRequiredUserId(), body));
    }

    @GetMapping("/audit")
    public ResponseEntity<?> audit() {
        return ResponseEntity.ok(admin.recentAudits());
    }
}
