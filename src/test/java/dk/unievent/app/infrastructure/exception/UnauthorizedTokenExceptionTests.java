package dk.unievent.app.infrastructure.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnauthorizedTokenExceptionTests {

    @Test
    void shouldStoreMessage() {
        UnauthorizedTokenException ex = new UnauthorizedTokenException("Token reuse detected");
        assertEquals("Token reuse detected", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }
}
