package dk.unievent.app.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

// generates and validates HMAC-signed CSRF tokens. Token format: <uuid>-<hmac_sha256(uuid, secret)>.
// all string comparisons use constant-time equality to prevent timing attacks.
@Service
public class CsrfTokenService {

    private final String csrfSecret;

    public CsrfTokenService(@Value("${unievent.security.csrf.secret}") String csrfSecret) {
        this.csrfSecret = csrfSecret;
    }

    public String generateToken() {
        String nonce = UUID.randomUUID().toString();
        String signature = signNonce(nonce);
        return nonce + "-" + signature;
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String providedToken, String expectedToken) {
        if (providedToken == null || providedToken.isBlank() || expectedToken == null || expectedToken.isBlank()) {
            return false;
        }

        if (!constantTimeEquals(providedToken, expectedToken)) { // check it matches the session's stored token
            return false;
        }

        return isSignatureValid(providedToken); // also check it was signed by us, not forged
    }

    public boolean isTokenValid(String providedToken, String expectedToken) {
        return validateToken(providedToken, expectedToken);
    }

    private boolean isSignatureValid(String token) {
        int delimiter = token.lastIndexOf('-'); // 1. split at the last '-' to get nonce and signature
        if (delimiter <= 0 || delimiter == token.length() - 1) {
            return false;
        }

        String nonce = token.substring(0, delimiter);
        String providedSignature = token.substring(delimiter + 1);
        try {
            UUID.fromString(nonce); // 2. reject anything that isn't a valid UUID nonce
        } catch (IllegalArgumentException ex) {
            return false;
        }
        String expectedSignature = signNonce(nonce);

        return constantTimeEquals(providedSignature, expectedSignature); // 3. compare in constant time
    }

    private String signNonce(String nonce) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(csrfSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(nonce.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate CSRF token signature", ex);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual( // MessageDigest.isEqual is constant-time, String.equals is not
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
