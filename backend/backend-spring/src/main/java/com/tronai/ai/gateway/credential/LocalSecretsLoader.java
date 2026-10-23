package com.tronai.ai.gateway.credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads KEY=VALUE pairs from a Git-ignored local file.
 * Never logs values. Path defaults to %LOCALAPPDATA%/TRON/secrets/ai-providers.env
 * or ~/.config/tron/secrets/ai-providers.env.
 */
public final class LocalSecretsLoader {

    private static final Logger log = LoggerFactory.getLogger(LocalSecretsLoader.class);

    private LocalSecretsLoader() {
    }

    public static Path defaultPath() {
        String override = System.getenv("TRON_AI_SECRETS_FILE");
        if (override != null && !override.isBlank()) {
            return Path.of(override.trim());
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            Path base = local == null || local.isBlank()
                    ? Path.of(System.getProperty("user.home"), "AppData", "Local")
                    : Path.of(local);
            return base.resolve("TRON").resolve("secrets").resolve("ai-providers.env");
        }
        return Path.of(System.getProperty("user.home"), ".config", "tron", "secrets", "ai-providers.env");
    }

    public static Map<String, String> load(Path path) {
        Map<String, String> values = new LinkedHashMap<>();
        if (path == null || !Files.isRegularFile(path)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isBlank() && !value.isBlank()) {
                    values.put(key, value);
                }
            }
            log.info("Loaded {} AI provider secret keys from local secrets file", values.size());
        } catch (IOException e) {
            log.warn("Could not read local AI secrets file");
        }
        return values;
    }
}
