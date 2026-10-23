package com.tronai.cv.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.ai.model.ModelManager;
import com.tronai.cv.dto.EducationItemDto;
import com.tronai.cv.dto.ExperienceItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CvParserTest {

    @Mock
    private ModelManager modelManager;

    private CvParser cvParser;

    @BeforeEach
    void setUp() {
        cvParser = new CvParser(modelManager, new ObjectMapper());
    }

    @Test
    @DisplayName("parseJsonResponse correctly deserializes structured CV payload")
    void parseValidJsonResponse() {
        String json = """
                {
                  "education": [
                    { "school": "Tokyo University", "period": "2018-2022", "detail": "B.S. Computer Science" }
                  ],
                  "skills": [ "Java", "Spring Boot", "MySQL", "Docker", "Java" ],
                  "work_experience": [
                    { "organization": "Tech Corp", "role": "Backend Engineer", "period": "2022-Present", "detail": "Developed microservices" }
                  ]
                }
                """;

        CvParsedResult result = cvParser.parseJsonResponse(json);

        assertThat(result).isNotNull();

        assertThat(result.getEducation()).hasSize(1);
        EducationItemDto edu = result.getEducation().get(0);
        assertThat(edu.getSchool()).isEqualTo("Tokyo University");
        assertThat(edu.getPeriod()).isEqualTo("2018-2022");
        assertThat(edu.getDetail()).isEqualTo("B.S. Computer Science");

        // Deduplication test (Java appears once)
        assertThat(result.getSkills()).containsExactly("Java", "Spring Boot", "MySQL", "Docker");

        assertThat(result.getWorkExperience()).hasSize(1);
        ExperienceItemDto exp = result.getWorkExperience().get(0);
        assertThat(exp.getOrganization()).isEqualTo("Tech Corp");
        assertThat(exp.getRole()).isEqualTo("Backend Engineer");
        assertThat(exp.getPeriod()).isEqualTo("2022-Present");
        assertThat(exp.getDetail()).isEqualTo("Developed microservices");
    }

    @Test
    @DisplayName("parseJsonResponse enforces field clipping and maximum item limits")
    void enforcesClippingAndLimits() {
        String longString = "A".repeat(400);
        StringBuilder skillsArray = new StringBuilder("[");
        for (int i = 0; i < 40; i++) {
            if (i > 0) skillsArray.append(",");
            skillsArray.append("\"Skill_").append(i).append("\"");
        }
        skillsArray.append("]");

        String json = String.format("""
                {
                  "education": [
                    { "school": "%s", "period": "2020", "detail": "Detail" }
                  ],
                  "skills": %s,
                  "work_experience": []
                }
                """, longString, skillsArray);

        CvParsedResult result = cvParser.parseJsonResponse(json);

        assertThat(result).isNotNull();
        // Capped at 300 chars
        assertThat(result.getEducation().get(0).getSchool().length()).isEqualTo(300);
        // Capped at 30 skills
        assertThat(result.getSkills()).hasSize(30);
    }

    @Test
    @DisplayName("parseDeterministic correctly extracts structured sections from plain text")
    void parseDeterministicSections() {
        String resumeText = """
                Taro Yamada
                Software Engineer
                
                Education:
                Waseda University, 2016-2020, Engineering
                
                Skills:
                Java, Kotlin, Spring Cloud, PostgreSQL
                
                Work Experience:
                LINE Yahoo, Lead Engineer, 2020-2024, Architecture design
                """;

        CvParsedResult result = cvParser.parseDeterministic(resumeText);

        assertThat(result).isNotNull();
        assertThat(result.getEducation()).hasSize(1);
        assertThat(result.getEducation().get(0).getSchool()).isEqualTo("Waseda University");
        assertThat(result.getEducation().get(0).getPeriod()).isEqualTo("2016-2020");
        assertThat(result.getEducation().get(0).getDetail()).isEqualTo("Engineering");

        assertThat(result.getSkills()).contains("Java", "Kotlin", "Spring Cloud", "PostgreSQL");

        assertThat(result.getWorkExperience()).hasSize(1);
        assertThat(result.getWorkExperience().get(0).getOrganization()).isEqualTo("LINE Yahoo");
        assertThat(result.getWorkExperience().get(0).getRole()).isEqualTo("Lead Engineer");
    }

    @Test
    @DisplayName("parse on unstructured random text returns empty items and transparent note")
    void parseUnstructuredTextReturnsNote() {
        String unstructured = "This is a random paragraph without any resume sections or facts.";

        CvParsedResult result = cvParser.parse(unstructured, "random.pdf");

        assertThat(result.getEducation()).isEmpty();
        assertThat(result.getSkills()).isEmpty();
        assertThat(result.getWorkExperience()).isEmpty();
        assertThat(result.getExtractionNotes()).contains(CvParser.STRUCTURING_FAILED_NOTE);
    }
}
