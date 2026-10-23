package com.tronai.language;

import java.util.Locale;
import java.util.Map;

/**
 * The one selected AI language. UI translation stays in the frontend.
 * Codes match the global selector: en, ja, si, bn, ne, my.
 */
public enum ExperienceLanguage {
    EN("en", "English"),
    JA("ja", "Japanese"),
    SI("si", "Sinhala"),
    BN("bn", "Bengali"),
    NE("ne", "Nepali"),
    MY("my", "Myanmar");

    private static final Map<String, ExperienceLanguage> ALIASES = Map.ofEntries(
            Map.entry("en", EN),
            Map.entry("english", EN),
            Map.entry("ja", JA),
            Map.entry("jp", JA),
            Map.entry("japanese", JA),
            Map.entry("si", SI),
            Map.entry("sinhala", SI),
            Map.entry("sin", SI),
            Map.entry("bn", BN),
            Map.entry("bengali", BN),
            Map.entry("bangla", BN),
            Map.entry("ne", NE),
            Map.entry("nepali", NE),
            Map.entry("my", MY),
            Map.entry("myanmar", MY),
            Map.entry("burmese", MY)
    );

    private final String code;
    private final String englishName;

    ExperienceLanguage(String code, String englishName) {
        this.code = code;
        this.englishName = englishName;
    }

    public String code() {
        return code;
    }

    public String englishName() {
        return englishName;
    }

    /**
     * Parses a requested language. Blank input is absent. Unknown input is absent.
     * Legacy interview values {@code japanese} and {@code english} map to ja and en.
     */
    public static ExperienceLanguage parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ALIASES.get(value.trim().toLowerCase(Locale.ROOT));
    }

    public static ExperienceLanguage parseOrDefault(String value, ExperienceLanguage fallback) {
        ExperienceLanguage parsed = parse(value);
        return parsed == null ? fallback : parsed;
    }

    public static String supportedList() {
        return "en, ja, si, bn, ne, or my";
    }
}
