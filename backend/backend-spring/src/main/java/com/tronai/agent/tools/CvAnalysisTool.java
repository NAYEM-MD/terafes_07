package com.tronai.agent.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.agent.model.AgentAction;
import com.tronai.agent.model.AgentTool;
import com.tronai.agent.model.AgentToolContext;
import com.tronai.agent.model.AgentToolResult;
import com.tronai.cv.CvDocumentEntity;
import com.tronai.cv.CvDocumentRepository;
import com.tronai.cv.CvProfileEntity;
import com.tronai.cv.CvProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool 1: CV Analysis Tool.
 * Extracts and normalizes candidate qualifications from stored CV metadata.
 * Strictly enforces user ownership and never hallucinates candidate experience.
 */
@Component
public class CvAnalysisTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(CvAnalysisTool.class);

    private final CvDocumentRepository cvDocumentRepository;
    private final CvProfileRepository cvProfileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CvAnalysisTool(CvDocumentRepository cvDocumentRepository, CvProfileRepository cvProfileRepository) {
        this.cvDocumentRepository = cvDocumentRepository;
        this.cvProfileRepository = cvProfileRepository;
    }

    @Override
    public String name() {
        return AgentAction.CV_ANALYSIS.name();
    }

    @Override
    public String description() {
        return "Analyzes candidate CV profile, extracting verified skills, education, and work experience.";
    }

    @Override
    public Set<String> requiredInputs() {
        return Collections.emptySet();
    }

    @Override
    public AgentToolResult execute(AgentToolContext context) {
        Long targetCvId = context.getState().getCvId();
        Long userId = context.getUserId();

        CvDocumentEntity doc = null;

        if (targetCvId != null) {
            // Find specific CV and verify ownership
            Optional<CvDocumentEntity> optDoc = cvDocumentRepository.findById(targetCvId);
            if (optDoc.isPresent()) {
                doc = optDoc.get();
                if (userId != null && doc.getUser() != null && !doc.getUser().getId().equals(userId)) {
                    log.warn("Ownership violation: User [{}] attempted to access CV [{}] owned by [{}]",
                            userId, targetCvId, doc.getUser().getId());
                    throw new AccessDeniedException("Access denied: You do not own CV ID " + targetCvId);
                }
            } else {
                return AgentToolResult.builder()
                        .success(false)
                        .summary("Specified CV document ID " + targetCvId + " was not found.")
                        .observationKey("candidateProfile")
                        .observationData(Map.of("status", "error", "reason", "cv_not_found"))
                        .build();
            }
        } else if (userId != null) {
            // Pick most recent CV for this user if available
            List<CvDocumentEntity> userDocs = cvDocumentRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (!userDocs.isEmpty()) {
                doc = userDocs.get(0);
                log.info("Auto-selected most recent CV [{}] for user [{}]", doc.getId(), userId);
            }
        }

        if (doc == null) {
            log.info("No CV supplied or found for run [{}]. Proceeding without candidate CV.", context.getState().getRunId());
            Map<String, Object> skippedData = new LinkedHashMap<>();
            skippedData.put("status", "skipped");
            skippedData.put("reason", "no_cv_supplied");
            skippedData.put("skills", Collections.emptyList());
            skippedData.put("candidateStrengthSignals", Collections.emptyList());

            return AgentToolResult.builder()
                    .success(true)
                    .status("skipped")
                    .summary("No candidate CV was supplied. Proceeding using general role requirements.")
                    .observationKey("candidateProfile")
                    .observationData(skippedData)
                    .build();
        }

        // Fetch stored profile
        Optional<CvProfileEntity> optProfile = cvProfileRepository.findByDocumentId(doc.getId());
        List<String> skills = new ArrayList<>();
        List<Map<String, Object>> education = new ArrayList<>();
        List<Map<String, Object>> workExperience = new ArrayList<>();
        List<String> extractionNotes = new ArrayList<>();

        if (optProfile.isPresent()) {
            CvProfileEntity profile = optProfile.get();
            skills = parseStringList(profile.getSkillsJson());
            education = parseMapList(profile.getEducationJson());
            workExperience = parseMapList(profile.getWorkExperienceJson());
            extractionNotes = parseStringList(profile.getExtractionNotesJson());
        }

        Map<String, Object> candidateData = new LinkedHashMap<>();
        candidateData.put("status", "analyzed");
        candidateData.put("cvId", doc.getId());
        candidateData.put("filename", doc.getOriginalFilename());
        candidateData.put("skills", skills);
        candidateData.put("education", education);
        candidateData.put("workExperience", workExperience);
        candidateData.put("certifications", extractionNotes);
        candidateData.put("candidateStrengthSignals", skills);
        candidateData.put("unknownFields", List.of("preferredSalary", "noticePeriod"));

        String summary = String.format("Analyzed CV '%s' (%d verified skills, %d work experience entries).",
                doc.getOriginalFilename(), skills.size(), workExperience.size());

        return AgentToolResult.builder()
                .success(true)
                .summary(summary)
                .observationKey("candidateProfile")
                .observationData(candidateData)
                .build();
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> parseMapList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
