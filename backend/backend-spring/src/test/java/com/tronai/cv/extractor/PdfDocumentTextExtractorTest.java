package com.tronai.cv.extractor;

import com.tronai.cv.CvException;
import com.tronai.cv.PdfTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfDocumentTextExtractorTest {

    private PdfDocumentTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PdfDocumentTextExtractor();
    }

    @Test
    @DisplayName("supports accepts valid PDF filenames and MIME types")
    void supportsValidPdf() {
        assertThat(extractor.supports("resume.pdf", "application/pdf")).isTrue();
        assertThat(extractor.supports("my_cv.PDF", "application/octet-stream")).isTrue();
        assertThat(extractor.supports("cv.pdf", null)).isTrue();
        assertThat(extractor.supports("cv.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")).isFalse();
        assertThat(extractor.supports("resume.txt", "text/plain")).isFalse();
    }

    @Test
    @DisplayName("extract successfully extracts text and page count from valid PDF")
    void extractValidPdf() throws Exception {
        String content = "Taro Yamada is a Senior Software Engineer with extensive Java and Spring Boot experience.";
        byte[] pdfBytes = PdfTestUtils.createPdf(content, 1);

        ExtractedDocument doc = extractor.extract(pdfBytes, "resume.pdf");

        assertThat(doc.getPageCount()).isEqualTo(1);
        assertThat(doc.getText()).contains("Taro Yamada");
        assertThat(doc.getCharacterCount()).isGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("extract rejects non-PDF data missing %PDF- header with HTTP 400")
    void rejectsNonPdfHeader() {
        byte[] invalidBytes = "This is not a PDF file at all".getBytes();

        assertThatThrownBy(() -> extractor.extract(invalidBytes, "invalid.pdf"))
                .isInstanceOf(CvException.class)
                .satisfies(ex -> {
                    CvException cvEx = (CvException) ex;
                    assertThat(cvEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(cvEx.getMessage()).isEqualTo("The file is not a valid PDF");
                });
    }

    @Test
    @DisplayName("extract rejects empty data with HTTP 400")
    void rejectsEmptyData() {
        assertThatThrownBy(() -> extractor.extract(new byte[0], "empty.pdf"))
                .isInstanceOf(CvException.class)
                .satisfies(ex -> assertThat(((CvException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("extract rejects PDF with text less than 20 characters with HTTP 422")
    void rejectsTooShortText() throws Exception {
        byte[] shortPdf = PdfTestUtils.createPdf("Short", 1);

        assertThatThrownBy(() -> extractor.extract(shortPdf, "short.pdf"))
                .isInstanceOf(CvException.class)
                .satisfies(ex -> {
                    CvException cvEx = (CvException) ex;
                    assertThat(cvEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(cvEx.getMessage()).contains("このPDFからテキストを抽出できませんでした");
                });
    }

    @Test
    @DisplayName("extract rejects encrypted PDF with HTTP 400")
    void rejectsEncryptedPdf() throws Exception {
        byte[] encryptedPdf = PdfTestUtils.createEncryptedPdf("Secret content that is long enough to pass min chars", "secret");

        assertThatThrownBy(() -> extractor.extract(encryptedPdf, "secret.pdf"))
                .isInstanceOf(CvException.class)
                .satisfies(ex -> {
                    CvException cvEx = (CvException) ex;
                    assertThat(cvEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(cvEx.getMessage()).isEqualTo("Encrypted PDFs are not supported");
                });
    }

    @Test
    @DisplayName("extract rejects PDF exceeding 15 pages with HTTP 400")
    void rejectsTooManyPages() throws Exception {
        byte[] multiPagePdf = PdfTestUtils.createPdf("Sample content for multi page testing", 16);

        assertThatThrownBy(() -> extractor.extract(multiPagePdf, "large.pdf"))
                .isInstanceOf(CvException.class)
                .satisfies(ex -> {
                    CvException cvEx = (CvException) ex;
                    assertThat(cvEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(cvEx.getMessage()).isEqualTo("The PDF has too many pages");
                });
    }

    @Test
    @DisplayName("extract rejects payload exceeding 5MB with HTTP 413")
    void rejectsPayloadExceeding5Mb() {
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        oversized[0] = '%'; oversized[1] = 'P'; oversized[2] = 'D'; oversized[3] = 'F'; oversized[4] = '-';

        assertThatThrownBy(() -> extractor.extract(oversized, "big.pdf"))
                .isInstanceOf(CvException.class)
                .satisfies(ex -> {
                    CvException cvEx = (CvException) ex;
                    assertThat(cvEx.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
                    assertThat(cvEx.getMessage()).isEqualTo("PDF exceeds the 5MB limit");
                });
    }
}
