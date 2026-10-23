package com.tronai.cv.extractor;

import com.tronai.cv.CvException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Text extractor for PDF documents using Apache PDFBox.
 * Replicates legacy FastAPI validations:
 * - 5MB max payload
 * - %PDF- magic bytes check
 * - Encryption rejection
 * - 1 to 15 pages validation
 * - Minimum 20 extracted characters validation
 */
@Component
public class PdfDocumentTextExtractor implements DocumentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfDocumentTextExtractor.class);

    public static final int MAX_PDF_BYTES = 5 * 1024 * 1024;
    public static final int MAX_PDF_PAGES = 15;
    public static final int MIN_PDF_TEXT_CHARS = 20;

    @Override
    public boolean supports(String filename, String contentType) {
        String name = filename != null ? filename.toLowerCase() : "";
        String type = contentType != null ? contentType.split(";")[0].trim().toLowerCase() : "";
        boolean isPdfName = name.endsWith(".pdf");
        return "application/pdf".equals(type) || (isPdfName && ("application/octet-stream".equals(type) || type.isEmpty()));
    }

    @Override
    public ExtractedDocument extract(byte[] data, String filename) {
        if (data == null || data.length == 0) {
            throw new CvException(HttpStatus.BAD_REQUEST, "The file is not a valid PDF");
        }

        if (data.length > MAX_PDF_BYTES) {
            throw new CvException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF exceeds the 5MB limit");
        }

        // Check %PDF- header
        if (data.length < 5 || data[0] != '%' || data[1] != 'P' || data[2] != 'D' || data[3] != 'F' || data[4] != '-') {
            throw new CvException(HttpStatus.BAD_REQUEST, "The file is not a valid PDF");
        }

        PDDocument document;
        try {
            document = Loader.loadPDF(data);
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            log.info("Encrypted PDF rejected: {}", e.getMessage());
            throw new CvException(HttpStatus.BAD_REQUEST, "Encrypted PDFs are not supported");
        } catch (Exception e) {
            log.info("PDF structure could not be read: {}", e.getMessage());
            throw new CvException(HttpStatus.BAD_REQUEST, "The PDF structure could not be read");
        }

        try {
            if (document.isEncrypted()) {
                throw new CvException(HttpStatus.BAD_REQUEST, "Encrypted PDFs are not supported");
            }

            int pageCount = document.getNumberOfPages();
            if (pageCount < 1) {
                throw new CvException(HttpStatus.BAD_REQUEST, "The PDF has no pages");
            }
            if (pageCount > MAX_PDF_PAGES) {
                throw new CvException(HttpStatus.BAD_REQUEST, "The PDF has too many pages");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null) {
                text = "";
            }
            text = text.strip();

            if (text.length() < MIN_PDF_TEXT_CHARS) {
                throw new CvException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "このPDFからテキストを抽出できませんでした。スキャン画像のみのPDFの可能性があります。テキストを選択できるPDFをアップロードしてください。"
                );
            }

            return new ExtractedDocument(text, pageCount, text.length());

        } catch (IOException e) {
            log.warn("Error reading PDF text: {}", e.getMessage());
            throw new CvException(HttpStatus.BAD_REQUEST, "The PDF structure could not be read");
        } finally {
            try {
                document.close();
            } catch (IOException ignored) {
            }
        }
    }
}
