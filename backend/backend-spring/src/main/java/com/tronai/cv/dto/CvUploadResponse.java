package com.tronai.cv.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Frontend-compatible CV upload response payload.
 * Matches legacy FastAPI schema:
 * {
 *   "filename": "...",
 *   "page_count": 1,
 *   "text_characters": 123,
 *   "education": [ ... ],
 *   "skills": [ ... ],
 *   "work_experience": [ ... ],
 *   "extraction_notes": [ ... ]
 * }
 */
public class CvUploadResponse {

    private Long cvId;

    private String filename;

    @JsonProperty("page_count")
    private int pageCount;

    @JsonProperty("text_characters")
    private int textCharacters;

    private List<EducationItemDto> education = new ArrayList<>();

    private List<String> skills = new ArrayList<>();

    @JsonProperty("work_experience")
    private List<ExperienceItemDto> workExperience = new ArrayList<>();

    @JsonProperty("extraction_notes")
    private List<String> extractionNotes = new ArrayList<>();

    public CvUploadResponse() {
    }

    public CvUploadResponse(String filename, int pageCount, int textCharacters,
                            List<EducationItemDto> education, List<String> skills,
                            List<ExperienceItemDto> workExperience, List<String> extractionNotes) {
        this.filename = filename != null ? filename : "";
        this.pageCount = pageCount;
        this.textCharacters = textCharacters;
        this.education = education != null ? new ArrayList<>(education) : new ArrayList<>();
        this.skills = skills != null ? new ArrayList<>(skills) : new ArrayList<>();
        this.workExperience = workExperience != null ? new ArrayList<>(workExperience) : new ArrayList<>();
        this.extractionNotes = extractionNotes != null ? new ArrayList<>(extractionNotes) : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getCvId() {
        return cvId;
    }

    public void setCvId(Long cvId) {
        this.cvId = cvId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public int getTextCharacters() {
        return textCharacters;
    }

    public void setTextCharacters(int textCharacters) {
        this.textCharacters = textCharacters;
    }

    public List<EducationItemDto> getEducation() {
        return education != null ? Collections.unmodifiableList(education) : Collections.emptyList();
    }

    public void setEducation(List<EducationItemDto> education) {
        this.education = education != null ? new ArrayList<>(education) : new ArrayList<>();
    }

    public List<String> getSkills() {
        return skills != null ? Collections.unmodifiableList(skills) : Collections.emptyList();
    }

    public void setSkills(List<String> skills) {
        this.skills = skills != null ? new ArrayList<>(skills) : new ArrayList<>();
    }

    public List<ExperienceItemDto> getWorkExperience() {
        return workExperience != null ? Collections.unmodifiableList(workExperience) : Collections.emptyList();
    }

    public void setWorkExperience(List<ExperienceItemDto> workExperience) {
        this.workExperience = workExperience != null ? new ArrayList<>(workExperience) : new ArrayList<>();
    }

    public List<String> getExtractionNotes() {
        return extractionNotes != null ? Collections.unmodifiableList(extractionNotes) : Collections.emptyList();
    }

    public void setExtractionNotes(List<String> extractionNotes) {
        this.extractionNotes = extractionNotes != null ? new ArrayList<>(extractionNotes) : new ArrayList<>();
    }

    public static class Builder {
        private Long cvId;
        private String filename = "";
        private int pageCount;
        private int textCharacters;
        private List<EducationItemDto> education = new ArrayList<>();
        private List<String> skills = new ArrayList<>();
        private List<ExperienceItemDto> workExperience = new ArrayList<>();
        private List<String> extractionNotes = new ArrayList<>();

        public Builder cvId(Long cvId) {
            this.cvId = cvId;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder pageCount(int pageCount) {
            this.pageCount = pageCount;
            return this;
        }

        public Builder textCharacters(int textCharacters) {
            this.textCharacters = textCharacters;
            return this;
        }

        public Builder education(List<EducationItemDto> education) {
            this.education = education != null ? new ArrayList<>(education) : new ArrayList<>();
            return this;
        }

        public Builder skills(List<String> skills) {
            this.skills = skills != null ? new ArrayList<>(skills) : new ArrayList<>();
            return this;
        }

        public Builder workExperience(List<ExperienceItemDto> workExperience) {
            this.workExperience = workExperience != null ? new ArrayList<>(workExperience) : new ArrayList<>();
            return this;
        }

        public Builder extractionNotes(List<String> extractionNotes) {
            this.extractionNotes = extractionNotes != null ? new ArrayList<>(extractionNotes) : new ArrayList<>();
            return this;
        }

        public CvUploadResponse build() {
            CvUploadResponse response = new CvUploadResponse(filename, pageCount, textCharacters, education, skills, workExperience, extractionNotes);
            response.setCvId(cvId);
            return response;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CvUploadResponse that)) return false;
        return pageCount == that.pageCount &&
                textCharacters == that.textCharacters &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(education, that.education) &&
                Objects.equals(skills, that.skills) &&
                Objects.equals(workExperience, that.workExperience) &&
                Objects.equals(extractionNotes, that.extractionNotes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, pageCount, textCharacters, education, skills, workExperience, extractionNotes);
    }

    @Override
    public String toString() {
        return "CvUploadResponse{" +
                "filename='" + filename + '\'' +
                ", pageCount=" + pageCount +
                ", textCharacters=" + textCharacters +
                ", education=" + education +
                ", skills=" + skills +
                ", workExperience=" + workExperience +
                ", extractionNotes=" + extractionNotes +
                '}';
    }
}
