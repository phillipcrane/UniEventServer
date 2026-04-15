package dk.unievent.app.infrastructure.util;

/**
 * Security utilities for Facebook integration.
 * Masks sensitive tokens in logs to prevent exposure.
 */
public class FacebookAppSecurityUtil {

    /**
     * Mask a sensitive token for safe logging.
     * Shows only a small prefix followed by a fixed mask marker to prevent
     * full token exposure in logs, regardless of token length.
     *
     * @param token The token to mask
     * @return Masked token string (e.g., "toke***") or "[EMPTY]" if null/blank
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "[EMPTY]";
        }

        int visibleChars = Math.min(4, token.length() - 1);
        String prefix = visibleChars > 0 ? token.substring(0, visibleChars) : "";
        return prefix + "***";
    }

    /**
     * Mask a sensitive credential for safe logging.
     * Generic masking for any credential type (secret, key, password).
     *
     * @param credential The credential to mask
     * @return Masked credential string
     */
    public static String maskCredential(String credential) {
        return maskToken(credential);
    }

    /**
     * Check if a token should be logged (always false for sensitive data).
     * Can be extended for log level-based decisions in future.
     *
     * @return Always false - tokens should never be logged in full
     */
    public static boolean shouldLogFullToken() {
        return false;
    }
}
