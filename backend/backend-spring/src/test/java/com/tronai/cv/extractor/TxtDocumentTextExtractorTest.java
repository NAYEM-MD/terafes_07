package com.tronai.cv.extractor;

import com.tronai.cv.CvException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TxtDocumentTextExtractorTest {

    private TxtDocumentTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new TxtDocumentTextExtractor();
    }

    @Test
    @DisplayName("supports accepts valid .txt filenames and text/plain")
    void supportsValidTxt() {
        assertThat(extractor.supports("cv.txt", "text/plain")).isTrue();
        assertThat(extractor.supports("my_resume.TXT", null)).isTrue();
        assertThat(extractor.supports("resume.pdf", "application/pdf")).isFalse();
    }

    @Test
    @DisplayName("extract successfully extracts text and reports 1 page from valid TXT")
    void extractValidTxt() {
        String text = "Kenji Sato is a Full Stack Developer with 5 years experience in Java and TypeScript.";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument doc = extractor.extract(bytes, "cv.txt");

        assertThat(doc.getPageCount()).isEqualTo(1);
        assertThat(doc.getText()).isEqualTo(text);
        assertThat(doc.getCharacterCount()).isEqualTo(text.length());
    }

    @Test
    @DisplayName("extract rejects empty TXT with HTTP 400")
    void rejectsEmptyTxt() {
        assertThatThrownBy(() -> extractor.extract(new byte[0], "empty.txt"))
                .isInstanceOf(CvException.class)
                .satisfies(ex -> assertThat(((CvException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("extract rejects TXT under 20 characters with HTTP 422")
    void rejectsShortTxt() {
        byte[] bytes = "Too short".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> extractor.extract(bytes, "short.txt"))
                .isInstanceOf(CvException.class)
                .satisfies(ex -> assertThat(((CvException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }
}
