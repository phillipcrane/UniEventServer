package dk.unievent.app.infrastructure.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FacebookAppSecurityUtilTests {

    @Test
    void maskTokenShouldReturnEmptyMarkerForNull() {
        assertEquals("[EMPTY]", FacebookAppSecurityUtil.maskToken(null));
    }

    @Test
    void maskTokenShouldReturnEmptyMarkerForEmptyString() {
        assertEquals("[EMPTY]", FacebookAppSecurityUtil.maskToken(""));
    }

    @Test
    void maskTokenShouldShowFirstFourCharsForNormalToken() {
        String result = FacebookAppSecurityUtil.maskToken("abcdefghij");
        assertEquals("abcd***", result);
    }

    @Test
    void maskTokenShouldHandleShortToken() {
        String result = FacebookAppSecurityUtil.maskToken("ab");
        assertEquals("a***", result);
    }

    @Test
    void maskTokenShouldHandleSingleCharToken() {
        String result = FacebookAppSecurityUtil.maskToken("x");
        assertEquals("***", result);
    }

    @Test
    void maskCredentialShouldBehaveLikeMaskToken() {
        assertEquals(FacebookAppSecurityUtil.maskToken("mySecret123"),
                     FacebookAppSecurityUtil.maskCredential("mySecret123"));
    }

    @Test
    void shouldLogFullTokenShouldAlwaysReturnFalse() {
        assertFalse(FacebookAppSecurityUtil.shouldLogFullToken());
    }
}
