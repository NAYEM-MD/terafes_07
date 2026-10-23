package com.tronai.desktopagent.task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evidence checks before accepting LLM finish:true or marking VERIFIED.
 * Does not hardcode product demo names; extracts file-like tokens from the goal text.
 */
final class AgentGoalVerifier {

    private static final Pattern FILE_TOKEN = Pattern.compile(
            "(?i)([\\w./\\\\-]+\\.(?:html?|css|js|ts|tsx|jsx|java|xml|json|md|txt|py|yml|yaml|toml|gradle|properties))");

    private AgentGoalVerifier() {
    }

    static boolean canAcceptFinish(DesktopAgentPlanner.TaskContext context) {
        List<String> required = requiredRelativePaths(context.goal());
        if (required.isEmpty()) {
            // No explicit files in the goal — require at least one successful file change or browser evidence.
            boolean fileEvidence = context.changedFiles() != null && !context.changedFiles().isEmpty();
            boolean browserEvidence = context.browserUrl() != null && !context.browserUrl().isBlank()
                    && context.lastObservation() != null && context.lastObservation().length() > 32;
            return fileEvidence || browserEvidence || context.stepCount() > 0 && !context.lastFailed();
        }
        Path root = Path.of(context.workspace()).toAbsolutePath().normalize();
        Set<String> changed = new LinkedHashSet<>();
        if (context.changedFiles() != null) {
            for (String c : context.changedFiles()) {
                if (c != null) {
                    changed.add(c.replace('\\', '/').toLowerCase(Locale.ROOT));
                }
            }
        }
        for (String rel : required) {
            Path target = root.resolve(rel).normalize();
            if (!target.startsWith(root)) {
                return false;
            }
            if (!Files.isRegularFile(target)) {
                // Also accept if a changed-file path ends with the required name.
                String needle = rel.replace('\\', '/').toLowerCase(Locale.ROOT);
                boolean mentioned = changed.stream().anyMatch(c -> c.endsWith(needle) || c.contains("/" + needle));
                if (!mentioned) {
                    return false;
                }
            }
        }
        return true;
    }

    static List<String> requiredRelativePaths(String goal) {
        List<String> out = new ArrayList<>();
        if (goal == null || goal.isBlank()) {
            return out;
        }
        Matcher matcher = FILE_TOKEN.matcher(goal);
        while (matcher.find()) {
            String token = matcher.group(1).replace('\\', '/');
            // Strip leading ./
            while (token.startsWith("./")) {
                token = token.substring(2);
            }
            if (!token.isBlank() && !out.contains(token)) {
                out.add(token);
            }
        }
        return out;
    }

    static boolean browserGoalLooksStrong(DesktopAgentPlanner.TaskContext context) {
        if (context.browserUrl() == null || context.browserUrl().isBlank()) {
            return false;
        }
        String observation = context.lastObservation() == null ? "" : context.lastObservation();
        if (observation.length() < 64) {
            return false;
        }
        String goal = context.goal() == null ? "" : context.goal().toLowerCase(Locale.ROOT);
        if (goal.contains("screenshot") || goal.contains("capture")) {
            return observation.toLowerCase(Locale.ROOT).contains("screenshot")
                    || observation.contains("iVBOR") // png base64 magic often appears in evidence
                    || observation.length() > 200;
        }
        return true;
    }
}
