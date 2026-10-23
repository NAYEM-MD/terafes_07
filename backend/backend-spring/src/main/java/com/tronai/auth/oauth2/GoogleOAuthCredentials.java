package com.tronai.auth.oauth2;

/**
 * Decides whether Google OAuth is actually configured.
 * Both the client id and the client secret must be present.
 * The removed development fallback values are treated as not configured.
 */
public final class GoogleOAuthCredentials {

    static final String REMOVED_FAKE_CLIENT_ID = "tron-ai-dev.apps.googleusercontent.com";
    static final String REMOVED_FAKE_CLIENT_SECRET = "tron-ai-dev-secret";

    private GoogleOAuthCredentials() {
    }

    public static boolean isConfigured(String clientId, String clientSecret) {
        String id = normalize(clientId);
        String secret = normalize(clientSecret);
        if (id.isEmpty() || secret.isEmpty()) {
            return false;
        }
        return !isRemovedFake(id, secret);
    }

    public static boolean isRemovedFake(String clientId, String clientSecret) {
        String id = normalize(clientId);
        String secret = normalize(clientSecret);
        return REMOVED_FAKE_CLIENT_ID.equals(id) || REMOVED_FAKE_CLIENT_SECRET.equals(secret);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
