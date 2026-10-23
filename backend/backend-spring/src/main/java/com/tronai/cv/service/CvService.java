package com.tronai.cv.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.auth.SecurityUtils;
import com.tronai.auth.UserPrincipal;
import com.tronai.cv.*;
import com.tronai.cv.dto.*;
import com.tronai.cv.extractor.CompositeDocumentTextExtractor;
import com.tronai.cv.extractor.ExtractedDocument;
import com.tronai.cv.parser.CvParsedResult;
import com.tronai.cv.parser.CvParser;
import com.tronai.queue.BackgroundJobPublisher;
import com.tronai.storage.ObjectStorageService;
import com.tronai.user.User;
import com.tronai.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class CvService {

    private static final Logger log = LoggerFactory.getLogger(CvService.class);

    public static final long MAX_FILE_BYTES = 5 * 1024 * 1024; // 5 MB

    private final CompositeDocumentTextExtractor documentExtractor;
    private final CvParser cvParser;
    private final ObjectStorageService objectStorageService;
    private final CvDocumentRepository cvDocumentRepository;
    private final CvProfileRepository cvProfileRepository;
    private final UserRepository userRepository;
    private final BackgroundJobPublisher backgroundJobPublisher;
    private final ObjectMapper objectMapper;

    public CvService(CompositeDocumentTextExtractor documentExtractor,
                     CvParser cvParser,
                     ObjectStorageService objectStorageService,
                     CvDocumentRepository cvDocumentRepository,
                     CvProfileRepository cvProfileRepository,
                     UserRepository userRepository,
                     BackgroundJobPublisher backgroundJobPublisher,
                     ObjectMapper objectMapper) {
        this.documentExtractor = documentExtractor;
        this.cvParser = cvParser;
        this.objectStorageService = objectStorageService;
        this.cvDocumentRepository = cvDocumentRepository;
        this.cvProfileRepository = cvProfileRepository;
        this.userRepository = userRepository;
        this.backgroundJobPublisher = backgroundJobPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CvUploadResponse processCvUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CvException(HttpStatus.BAD_REQUEST, "The file is not a valid PDF");
        }

        if (file.getSize() > MAX_FILE_BYTES) {
            throw new CvException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF exceeds the 5MB limit");
        }

        String rawFilename = file.getOriginalFilename();
        String safeFilename = sanitizeFilename(rawFilename);
        String contentType = file.getContentType();

        if (!documentExtractor.supports(safeFilename, contentType)) {
            throw new CvException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF files are accepted");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read uploaded file bytes: {}", e.getMessage());
            throw new CvException(HttpStatus.BAD_REQUEST, "The PDF structure could not be read");
        }

        // Validate and extract raw document text and page metadata
        ExtractedDocument extracted = documentExtractor.extract(data, safeFilename, contentType);

        // Structure resume fields
        CvParsedResult parsed = cvParser.parse(extracted.getText(), safeFilename);

        String sha256 = calculateSha256(data);

        // Handle authenticated user persistence
        Optional<UserPrincipal> currentUser = SecurityUtils.getCurrentUser();
        Long cvId = null;

        if (currentUser.isPresent()) {
            Long userId = currentUser.get().getId();
            User user = userRepository.findById(userId).orElse(null);

            if (user != null) {
                String objectKey = String.format("users/%d/cv/%s.pdf", userId, UUID.randomUUID());
                boolean objectUploaded = false;

                try {
                    // Store original file in private MinIO storage
                    objectStorageService.putObject(objectKey, data, contentType);
                    objectUploaded = true;

                    // Persist CV Document metadata
                    CvDocumentEntity doc = CvDocumentEntity.builder()
                            .user(user)
                            .originalFilename(safeFilename)
                            .objectKey(objectKey)
                            .contentType(contentType != null ? contentType : "application/pdf")
                            .sizeBytes((long) data.length)
                            .sha256(sha256)
                            .pageCount(extracted.getPageCount())
                            .textCharacters(extracted.getCharacterCount())
                            .status("COMPLETED")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    doc = cvDocumentRepository.save(doc);
                    cvId = doc.getId();

                    // Persist parsed CV Profile
                    String educationJson = serializeJson(parsed.getEducation());
                    String skillsJson = serializeJson(parsed.getSkills());
                    String workExpJson = serializeJson(parsed.getWorkExperience());
                    String extractionNotesJson = serializeJson(parsed.getExtractionNotes());

                    CvProfileEntity profile = CvProfileEntity.builder()
                            .document(doc)
                            .educationJson(educationJson)
                            .skillsJson(skillsJson)
                            .workExperienceJson(workExpJson)
                            .extractionNotesJson(extractionNotesJson)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    cvProfileRepository.save(profile);

                    // Publish async non-critical queue event
                    backgroundJobPublisher.publishCvProcessed(userId, doc.getId(), safeFilename);

                } catch (Exception e) {
                    // Compensating deletion to prevent orphaned objects
                    if (objectUploaded) {
                        try {
                            log.warn("Performing compensating deletion for MinIO object: {}", objectKey);
                            objectStorageService.deleteObject(objectKey);
                        } catch (Exception delEx) {
                            log.error("Compensating deletion failed: {}", delEx.getMessage());
                        }
                    }
                    log.error("Failed to persist CV document / profile: {}", e.getMessage(), e);
                    throw new CvException(HttpStatus.INTERNAL_SERVER_ERROR, "CV storage failed: " + e.getMessage());
                }
            }
        }

        return CvUploadResponse.builder()
                .cvId(cvId)
                .filename(safeFilename)
                .pageCount(extracted.getPageCount())
                .textCharacters(extracted.getCharacterCount())
                .education(parsed.getEducation())
                .skills(parsed.getSkills())
                .workExperience(parsed.getWorkExperience())
                .extractionNotes(parsed.getExtractionNotes())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CvDocumentSummaryDto> listUserDocuments(Long userId) {
        List<CvDocumentEntity> docs = cvDocumentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<CvDocumentSummaryDto> result = new ArrayList<>();
        for (CvDocumentEntity d : docs) {
            result.add(CvDocumentSummaryDto.builder()
                    .id(d.getId())
                    .filename(d.getOriginalFilename())
                    .contentType(d.getContentType())
                    .sizeBytes(d.getSizeBytes())
                    .pageCount(d.getPageCount())
                    .textCharacters(d.getTextCharacters())
                    .status(d.getStatus())
                    .createdAt(d.getCreatedAt())
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CvDocumentDetailDto getUserDocument(Long userId, Long cvId) {
        CvDocumentEntity doc = cvDocumentRepository.findByIdAndUserId(cvId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV document not found"));

        CvProfileEntity profile = cvProfileRepository.findByDocumentId(doc.getId()).orElse(null);

        List<EducationItemDto> education = profile != null ? deserializeList(profile.getEducationJson(), new TypeReference<>() {}) : List.of();
        List<String> skills = profile != null ? deserializeList(profile.getSkillsJson(), new TypeReference<>() {}) : List.of();
        List<ExperienceItemDto> workExperience = profile != null ? deserializeList(profile.getWorkExperienceJson(), new TypeReference<>() {}) : List.of();
        List<String> extractionNotes = profile != null ? deserializeList(profile.getExtractionNotesJson(), new TypeReference<>() {}) : List.of();

        return CvDocumentDetailDto.builder()
                .id(doc.getId())
                .filename(doc.getOriginalFilename())
                .contentType(doc.getContentType())
                .sizeBytes(doc.getSizeBytes())
                .sha256(doc.getSha256())
                .pageCount(doc.getPageCount())
                .textCharacters(doc.getTextCharacters())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .education(education)
                .skills(skills)
                .workExperience(workExperience)
                .extractionNotes(extractionNotes)
                .build();
    }

    @Transactional
    public void deleteUserDocument(Long userId, Long cvId) {
        CvDocumentEntity doc = cvDocumentRepository.findByIdAndUserId(cvId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV document not found"));

        String objectKey = doc.getObjectKey();

        // Delete from database
        cvDocumentRepository.delete(doc);

        // Delete from MinIO storage
        if (objectKey != null && !objectKey.isBlank()) {
            try {
                objectStorageService.deleteObject(objectKey);
            } catch (Exception e) {
                log.warn("Failed to delete MinIO object for deleted CV {}: {}", cvId, e.getMessage());
            }
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upload.pdf";
        }
        try {
            return Path.of(filename).getFileName().toString();
        } catch (Exception e) {
            return "upload.pdf";
        }
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String serializeJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private <T> List<T> deserializeList(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            return List.of();
        }
    }
}
