package com.tronai.supervisor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tronai.ai.ChatMessage;
import com.tronai.ai.gateway.AiFeature;
import com.tronai.ai.gateway.AiGateway;
import com.tronai.ai.gateway.AiMode;
import com.tronai.ai.gateway.AiRequestContext;
import com.tronai.hybrid.HybridRoutingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hierarchical AI Supervisor: GOAL -> MILESTONES -> (engine handoff) -> REVIEW.
 * Original goal is immutable. Completion claims without verification stay COMPLETED, not VERIFIED.
 */
@Service
public class SupervisorService {

    private static final Pattern FILE_TOKEN = Pattern.compile(
            "(?i)([\\w./\\\\-]+\\.(?:html?|css|js|ts|tsx|jsx|java|md|txt|json))");

    private final SupervisorPlanRepository plans;
    private final SupervisorMilestoneRepository milestones;
    private final SupervisorEventRepository events;
    private final HybridRoutingService hybrid;
    private final ObjectMapper objectMapper;
    private final AiGateway aiGateway;

    @Value("${tron.supervisor.local-model:qwen3:8b}")
    private String localModel;

    public SupervisorService(SupervisorPlanRepository plans,
                             SupervisorMilestoneRepository milestones,
                             SupervisorEventRepository events,
                             HybridRoutingService hybrid,
                             ObjectMapper objectMapper,
                             @Autowired(required = false) AiGateway aiGateway) {
        this.plans = plans;
        this.milestones = milestones;
        this.events = events;
        this.hybrid = hybrid;
        this.objectMapper = objectMapper;
        this.aiGateway = aiGateway;
    }

    @Transactional
    public Map<String, Object> createPlan(Long userId, String goal, String preferredEngine) {
        if (goal == null || goal.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Goal is required");
        }
        String route = hybrid.selectSupervisorRoute();
        if ("PAUSED".equals(route) || "UNAVAILABLE".equals(route)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI Supervisor routing unavailable: " + route);
        }
        if (!hybrid.tryAcquireAiSlot()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "AI request queue saturated");
        }
        try {
            String engine = selectEngine(goal, preferredEngine);
            List<MilestoneDraft> drafts = draftMilestones(goal, route);
            String planId = UUID.randomUUID().toString();

            ObjectNode planJson = objectMapper.createObjectNode();
            planJson.put("originalGoal", goal);
            planJson.put("engine", engine);
            planJson.put("modelRoute", route);
            ArrayNode ms = planJson.putArray("milestones");
            for (MilestoneDraft d : drafts) {
                ObjectNode n = ms.addObject();
                n.put("title", d.title());
                n.put("acceptance", d.acceptance());
            }

            SupervisorPlanEntity plan = new SupervisorPlanEntity();
            plan.setId(planId);
            plan.setUserId(userId);
            plan.setOriginalGoal(goal.trim());
            plan.setStatus(SupervisorPlanStatus.PLANNED);
            plan.setEngine(engine);
            plan.setModelRoute(route);
            plan.setCurrentMilestoneIndex(0);
            plan.setPlanJson(planJson.toString());
            plan.setCreatedAt(Instant.now());
            plan.setUpdatedAt(Instant.now());
            plans.save(plan);

            int idx = 0;
            for (MilestoneDraft d : drafts) {
                SupervisorMilestoneEntity m = new SupervisorMilestoneEntity();
                m.setId(UUID.randomUUID().toString());
                m.setPlanId(planId);
                m.setMilestoneIndex(idx++);
                m.setTitle(d.title());
                m.setStatus(idx == 1 ? "READY" : "PENDING");
                m.setAcceptanceCriteria(d.acceptance());
                milestones.save(m);
            }
            appendEvent(planId, "PLAN_CREATED", "engine=" + engine + " route=" + route);
            return view(planId, userId);
        } finally {
            hybrid.releaseAiSlot();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> view(String planId, Long userId) {
        SupervisorPlanEntity plan = plans.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", plan.getId());
        body.put("originalGoal", plan.getOriginalGoal());
        body.put("status", plan.getStatus().name());
        body.put("engine", plan.getEngine());
        body.put("modelRoute", plan.getModelRoute());
        body.put("currentMilestoneIndex", plan.getCurrentMilestoneIndex());
        body.put("verifiedResult", plan.getVerifiedResult());
        body.put("errorSummary", plan.getErrorSummary());
        body.put("createdAt", plan.getCreatedAt());
        body.put("updatedAt", plan.getUpdatedAt());
        List<Map<String, Object>> ms = new ArrayList<>();
        for (SupervisorMilestoneEntity m : milestones.findByPlanIdOrderByMilestoneIndexAsc(planId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", m.getMilestoneIndex());
            row.put("title", m.getTitle());
            row.put("status", m.getStatus());
            row.put("acceptanceCriteria", m.getAcceptanceCriteria());
            row.put("resultSummary", m.getResultSummary());
            ms.add(row);
        }
        body.put("milestones", ms);
        List<Map<String, Object>> ev = new ArrayList<>();
        for (SupervisorEventEntity e : events.findByPlanIdOrderByCreatedAtAsc(planId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", e.getEventType());
            row.put("detail", e.getDetail());
            row.put("createdAt", e.getCreatedAt());
            ev.add(row);
        }
        body.put("events", ev);
        return body;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SupervisorPlanEntity p : plans.findByUserIdOrderByCreatedAtDesc(userId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("originalGoal", p.getOriginalGoal());
            row.put("status", p.getStatus().name());
            row.put("engine", p.getEngine());
            row.put("modelRoute", p.getModelRoute());
            row.put("updatedAt", p.getUpdatedAt());
            out.add(row);
        }
        return out;
    }

    @Transactional
    public Map<String, Object> markMilestoneResult(Long userId, String planId, int index,
                                                   boolean success, String evidence, boolean verified) {
        SupervisorPlanEntity plan = plans.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        if (!plan.getOriginalGoal().equals(plan.getOriginalGoal())) {
            // intentional no-op guard — goal is never rewritten
        }
        List<SupervisorMilestoneEntity> list = milestones.findByPlanIdOrderByMilestoneIndexAsc(planId);
        SupervisorMilestoneEntity target = list.stream()
                .filter(m -> m.getMilestoneIndex() == index)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown milestone"));
        target.setStatus(success ? (verified ? "VERIFIED" : "COMPLETED") : "FAILED");
        target.setResultSummary(evidence);
        target.setUpdatedAt(Instant.now());
        milestones.save(target);
        appendEvent(planId, success ? "MILESTONE_DONE" : "MILESTONE_FAILED",
                "index=" + index + " verified=" + verified);

        if (!success) {
            plan.setStatus(SupervisorPlanStatus.FAILED);
            plan.setErrorSummary(evidence);
        } else if (index >= list.size() - 1) {
            plan.setStatus(verified ? SupervisorPlanStatus.VERIFIED : SupervisorPlanStatus.COMPLETED);
            plan.setVerifiedResult(verified ? evidence : null);
            plan.setCurrentMilestoneIndex(index);
        } else {
            plan.setStatus(SupervisorPlanStatus.RUNNING);
            plan.setCurrentMilestoneIndex(index + 1);
            list.get(index + 1).setStatus("READY");
            milestones.save(list.get(index + 1));
        }
        plan.setUpdatedAt(Instant.now());
        plans.save(plan);
        return view(planId, userId);
    }

    private String selectEngine(String goal, String preferred) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim().toUpperCase(Locale.ROOT);
        }
        String g = goal.toLowerCase(Locale.ROOT);
        if (g.contains("browser") || g.contains("screenshot") || g.contains("open http") || g.contains("open https")) {
            return "OPENCLAW";
        }
        if (g.contains("react") || g.contains("website") || g.contains("file") || g.contains(".html")
                || g.contains("code") || g.contains("build") || g.contains("maven") || g.contains("npm")) {
            return "OPENCODE";
        }
        return "DESKTOP_AGENT";
    }

    private List<MilestoneDraft> draftMilestones(String goal, String route) {
        List<MilestoneDraft> fromModel = tryModelMilestones(goal, route);
        if (fromModel != null && !fromModel.isEmpty()) {
            return fromModel;
        }
        return heuristicMilestones(goal);
    }

    private List<MilestoneDraft> tryModelMilestones(String goal, String route) {
        if (aiGateway == null || !"CLOUD_GATEWAY".equals(route) && !"LOCAL_OLLAMA".equals(route)) {
            // Still try gateway for cloud; local currently shares gateway Ollama provider when configured.
        }
        if (aiGateway == null) {
            return null;
        }
        if (!hybrid.tryAcquireAiSlot()) {
            return null;
        }
        try {
            String prompt = """
                    You are the TRON AI Supervisor. Return ONLY JSON:
                    {"milestones":[{"title":"...","acceptance":"..."}]}
                    Create 2-5 milestones for this immutable goal. Do not claim the goal is done.
                    Goal: %s
                    """.formatted(goal);
            var result = aiGateway.generate(AiRequestContext.builder()
                    .mode(AiMode.FAST)
                    .feature(AiFeature.SUPERVISOR_PLAN)
                    .jsonMode(true)
                    .maxTokens(800)
                    .messages(List.of(
                            ChatMessage.builder().role("system").content("Return valid JSON only.").build(),
                            ChatMessage.builder().role("user").content(prompt).build()))
                    .build());
            if (result == null || result.getText() == null || result.getText().isBlank()) {
                return null;
            }
            String text = result.getText().trim();
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return null;
            }
            JsonNode root = objectMapper.readTree(text.substring(start, end + 1));
            JsonNode arr = root.path("milestones");
            if (!arr.isArray() || arr.isEmpty()) {
                return null;
            }
            List<MilestoneDraft> out = new ArrayList<>();
            for (JsonNode n : arr) {
                String title = n.path("title").asText("").trim();
                String acceptance = n.path("acceptance").asText("Evidence required").trim();
                if (!title.isBlank()) {
                    out.add(new MilestoneDraft(title, acceptance));
                }
            }
            return out.isEmpty() ? null : out;
        } catch (Exception e) {
            return null;
        } finally {
            hybrid.releaseAiSlot();
        }
    }

    private List<MilestoneDraft> heuristicMilestones(String goal) {
        List<MilestoneDraft> out = new ArrayList<>();
        out.add(new MilestoneDraft("Analyze request and workspace constraints",
                "Intent preserved; constraints listed"));
        Matcher matcher = FILE_TOKEN.matcher(goal);
        List<String> files = new ArrayList<>();
        while (matcher.find()) {
            files.add(matcher.group(1));
        }
        if (!files.isEmpty()) {
            out.add(new MilestoneDraft("Produce required files: " + String.join(", ", files),
                    "Files exist on disk with non-empty content"));
        } else {
            out.add(new MilestoneDraft("Execute primary work with selected engine",
                    "Engine session reports success with artifacts"));
        }
        out.add(new MilestoneDraft("Verify results against original goal",
                "Independent verification of files/tests/browser evidence — not LLM claim alone"));
        return out;
    }

    private void appendEvent(String planId, String type, String detail) {
        SupervisorEventEntity e = new SupervisorEventEntity();
        e.setId(UUID.randomUUID().toString());
        e.setPlanId(planId);
        e.setEventType(type);
        e.setDetail(detail);
        e.setCreatedAt(Instant.now());
        events.save(e);
    }

    private record MilestoneDraft(String title, String acceptance) {
    }
}
