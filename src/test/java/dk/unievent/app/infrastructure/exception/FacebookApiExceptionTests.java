package dk.unievent.app.infrastructure.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FacebookApiExceptionTests {

    @Test
    void shouldStoreStatusCodeAndErrorType() {
        FacebookApiException ex = new FacebookApiException("Token expired", 401, "OAuthException");
        assertEquals("Token expired", ex.getMessage());
        assertEquals(401, ex.getStatusCode());
        assertEquals("OAuthException", ex.getErrorType());
    }

    @Test
    void shouldStoreCause() {
        RuntimeException cause = new RuntimeException("root cause");
        FacebookApiException ex = new FacebookApiException("Wrapped", 500, "INTERNAL", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void toStringShouldIncludeAllFields() {
        FacebookApiException ex = new FacebookApiException("Bad token", 400, "InvalidToken");
        String str = ex.toString();
        assertTrue(str.contains("Bad token"));
        assertTrue(str.contains("400"));
        assertTrue(str.contains("InvalidToken"));
    }
}
