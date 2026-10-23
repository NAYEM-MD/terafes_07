package com.tronai.cv.parser;

import com.tronai.cv.dto.EducationItemDto;
import com.tronai.cv.dto.ExperienceItemDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CvParsedResult {

    private final List<EducationItemDto> education;
    private final List<String> skills;
    private final List<ExperienceItemDto> workExperience;
    private final List<String> extractionNotes;

    public CvParsedResult(List<EducationItemDto> education,
                          List<String> skills,
                          List<ExperienceItemDto> workExperience,
                          List<String> extractionNotes) {
        this.education = education != null ? new ArrayList<>(education) : new ArrayList<>();
        this.skills = skills != null ? new ArrayList<>(skills) : new ArrayList<>();
        this.workExperience = workExperience != null ? new ArrayList<>(workExperience) : new ArrayList<>();
        this.extractionNotes = extractionNotes != null ? new ArrayList<>(extractionNotes) : new ArrayList<>();
    }

    public List<EducationItemDto> getEducation() {
        return Collections.unmodifiableList(education);
    }

    public List<String> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public List<ExperienceItemDto> getWorkExperience() {
        return Collections.unmodifiableList(workExperience);
    }

    public List<String> getExtractionNotes() {
        return Collections.unmodifiableList(extractionNotes);
    }
}
