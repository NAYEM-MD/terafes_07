package com.tronai.cv.extractor;

/**
 * Abstraction for extracting raw text and document metadata from uploaded files.
 */
public interface DocumentTextExtractor {

    /**
     * Determines whether this extractor can process the given file based on filename and content type.
     */
    boolean supports(String filename, String contentType);

    /**
     * Extracts text, page count, and character count from file data.
     */
    ExtractedDocument extract(byte[] data, String filename);
}
