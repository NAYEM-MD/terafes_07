package com.tronai.ai.model;

import com.tronai.ai.model.dto.ModelResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only REST endpoint for discovering configured models.
 * Route: GET /api/v1/models
 */
@RestController
@RequestMapping("/api/v1")
public class ModelController {

    private final ModelManager modelManager;

    public ModelController(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    @GetMapping(value = "/models", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ModelResponse> getModels() {
        ModelResponse response = modelManager.getModelsResponse();
        return ResponseEntity.ok(response);
    }
}
