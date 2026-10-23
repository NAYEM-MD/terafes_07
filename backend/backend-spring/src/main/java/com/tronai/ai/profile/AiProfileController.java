package com.tronai.ai.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing available universal AI performance profiles (FAST, QUALITY).
 */
@RestController
@RequestMapping("/api/v1/ai-profiles")
@Tag(name = "AI Profiles", description = "Query available AI performance profiles and model tiers")
public class AiProfileController {

    private final AiProfileService aiProfileService;

    public AiProfileController(AiProfileService aiProfileService) {
        this.aiProfileService = aiProfileService;
    }

    @GetMapping
    @Operation(summary = "Get available AI profiles", description = "Returns FAST and QUALITY profiles with current availability and descriptions")
    public ResponseEntity<AiProfilesResponse> getProfiles() {
        return ResponseEntity.ok(aiProfileService.getAvailableProfiles());
    }
}
