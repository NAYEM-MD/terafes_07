package com.tronai.cv.extractor;

import java.util.Objects;

/**
 * Result of raw document text extraction.
 */
public class ExtractedDocument {

    private final String text;
    private final int pageCount;
    private final int characterCount;

    public ExtractedDocument(String text, int pageCount, int characterCount) {
        this.text = text != null ? text : "";
        this.pageCount = Math.max(1, pageCount);
        this.characterCount = characterCount >= 0 ? characterCount : (text != null ? text.length() : 0);
    }

    public String getText() {
        return text;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtractedDocument that)) return false;
        return pageCount == that.pageCount &&
                characterCount == that.characterCount &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, pageCount, characterCount);
    }

    @Override
    public String toString() {
        return "ExtractedDocument{" +
                "textLength=" + (text != null ? text.length() : 0) +
                ", pageCount=" + pageCount +
                ", characterCount=" + characterCount +
                '}';
    }
}
