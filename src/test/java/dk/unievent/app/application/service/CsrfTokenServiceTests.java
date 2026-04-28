package dk.unievent.app.application.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CsrfTokenServiceTests {

    private final CsrfTokenService csrfTokenService = new CsrfTokenService("test-csrf-secret-1234567890");

    @Test
    void generateTokenShouldReturnUuidSignatureFormat() {
        String token = csrfTokenService.generateToken();

        assertNotNull(token);
        int delimiter = token.lastIndexOf('-');
        assertTrue(delimiter > 0);
        assertTrue(delimiter < token.length() - 1);

        String nonce = token.substring(0, delimiter);
        String signature = token.substring(delimiter + 1);

        assertDoesNotThrow(() -> UUID.fromString(nonce));
        assertTrue(signature.matches("[0-9a-f]{64}"));
    }

    @Test
    void validateTokenShouldReturnTrueForMatchingToken() {
        String token = csrfTokenService.generateToken();

        assertTrue(csrfTokenService.validateToken(token, token));
    }

    @Test
    void validateTokenShouldReturnFalseForMismatchedTokens() {
        String token = csrfTokenService.generateToken();
        String otherToken = csrfTokenService.generateToken();

        assertFalse(csrfTokenService.validateToken(token, otherToken));
    }

    @Test
    void validateTokenShouldReturnFalseForNullOrBlankInputs() {
        String token = csrfTokenService.generateToken();

        assertFalse(csrfTokenService.validateToken(null, token));
        assertFalse(csrfTokenService.validateToken(token, null));
        assertFalse(csrfTokenService.validateToken("", token));
        assertFalse(csrfTokenService.validateToken(" ", token));
        assertFalse(csrfTokenService.validateToken(token, ""));
        assertFalse(csrfTokenService.validateToken(token, " "));
    }

    @Test
    void validateTokenShouldRejectInvalidUuidComponent() {
        String token = csrfTokenService.generateToken();
        int delimiter = token.lastIndexOf('-');
        String signature = token.substring(delimiter + 1);
        String invalidToken = "not-a-uuid-" + signature;

        assertFalse(csrfTokenService.validateToken(invalidToken, invalidToken));
    }

    @Test
    void validateTokenShouldRejectModifiedSignature() {
        String token = csrfTokenService.generateToken();
        char last = token.charAt(token.length() - 1);
        char replacement = last == 'a' ? 'b' : 'a';
        String tamperedToken = token.substring(0, token.length() - 1) + replacement;

        assertFalse(csrfTokenService.validateToken(tamperedToken, tamperedToken));
    }
}
