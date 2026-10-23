package com.tronai.cv.extractor;

import com.tronai.cv.CvException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Text extractor for plain text files (.txt).
 */
@Component
public class TxtDocumentTextExtractor implements DocumentTextExtractor {

    public static final int MAX_TXT_BYTES = 5 * 1024 * 1024;
    public static final int MIN_TXT_CHARS = 20;

    @Override
    public boolean supports(String filename, String contentType) {
        String name = filename != null ? filename.toLowerCase() : "";
        String type = contentType != null ? contentType.split(";")[0].trim().toLowerCase() : "";
        return name.endsWith(".txt") || "text/plain".equals(type);
    }

    @Override
    public ExtractedDocument extract(byte[] data, String filename) {
        if (data == null || data.length == 0) {
            throw new CvException(HttpStatus.BAD_REQUEST, "The file is empty");
        }

        if (data.length > MAX_TXT_BYTES) {
            throw new CvException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the 5MB limit");
        }

        String text = new String(data, StandardCharsets.UTF_8).strip();
        if (text.length() < MIN_TXT_CHARS) {
            throw new CvException(HttpStatus.UNPROCESSABLE_ENTITY, "Text content is too short to be a valid CV");
        }

        return new ExtractedDocument(text, 1, text.length());
    }
}
