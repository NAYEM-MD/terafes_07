package com.tronai.agent.controller;

import com.tronai.agent.dto.AgentDtos;
import com.tronai.agent.service.AgentService;
import com.tronai.auth.SecurityUtils;
import com.tronai.interview.dto.InterviewStartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for autonomous Agentic Interview Preparation runs, history, and interview initialization.
 */
@RestController
@RequestMapping("/api/v1/agents")
@Tag(name = "Agentic Interview Preparation", description = "Autonomous multi-step agent for interview preparation")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping(
            value = "/interview-prep/run",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Execute an autonomous multi-step interview preparation agent run")
    public ResponseEntity<AgentDtos.AgentRunResponse> runAgent(
            @Valid @RequestBody AgentDtos.AgentRunRequest request) {
        Long userId = SecurityUtils.getRequiredUserId();
        AgentDtos.AgentRunResponse response = agentService.runAgent(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/runs", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all agent preparation runs owned by the authenticated user")
    public ResponseEntity<List<AgentDtos.AgentRunResponse>> listRuns() {
        Long userId = SecurityUtils.getRequiredUserId();
        return ResponseEntity.ok(agentService.listUserRuns(userId));
    }

    @GetMapping(value = "/runs/{runId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve a specific agent run with full execution trace and result")
    public ResponseEntity<AgentDtos.AgentRunResponse> getRun(@PathVariable("runId") String runId) {
        Long userId = SecurityUtils.getRequiredUserId();
        return ResponseEntity.ok(agentService.getUserRun(userId, runId));
    }

    @DeleteMapping(value = "/runs/{runId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete an agent run owned by the authenticated user")
    public ResponseEntity<Map<String, String>> deleteRun(@PathVariable("runId") String runId) {
        Long userId = SecurityUtils.getRequiredUserId();
        agentService.deleteUserRun(userId, runId);
        return ResponseEntity.ok(Map.of("detail", "Agent run deleted successfully"));
    }

    @PostMapping(value = "/runs/{runId}/start-interview", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Launch an interactive interview session pre-configured from the agent result")
    public ResponseEntity<InterviewStartResponse> startInterviewFromRun(@PathVariable("runId") String runId) {
        Long userId = SecurityUtils.getRequiredUserId();
        InterviewStartResponse response = agentService.startInterviewFromRun(userId, runId);
        return ResponseEntity.ok(response);
    }
}
