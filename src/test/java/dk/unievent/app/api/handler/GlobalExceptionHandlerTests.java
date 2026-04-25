package dk.unievent.app.api.handler;

import dk.unievent.app.application.dto.EventDTO;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import dk.unievent.app.infrastructure.exception.EmailAlreadyRegisteredException;
import dk.unievent.app.infrastructure.exception.InvalidConfirmationTokenException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyAlreadyUsedException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyExpiredException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyNotFoundException;
import dk.unievent.app.infrastructure.exception.UsernameAlreadyTakenException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTests {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidationExceptionsShouldReturnFieldErrors() throws Exception {
        EventDTO target = new EventDTO();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(target, "eventDTO");
        binding.addError(new FieldError("eventDTO", "title", "Title is required"));
        new DummyController().create(target);

        Method method = DummyController.class.getDeclaredMethod("create", EventDTO.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, binding);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation Failed", response.getBody().get("error"));
        assertEquals(400, response.getBody().get("status"));

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertEquals("Title is required", errors.get("title"));
    }

    @Test
    void handleBadRequestShouldReturnBadRequestShape() throws Exception {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("name", "String");

        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().get("error"));
        assertEquals(400, response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }

    @Test
    void handleTypeMismatchShouldReturnBadRequest() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
            "abc", Long.class, "id", null, new IllegalArgumentException("bad id")
        );

        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().get("error"));
    }

    @Test
    void handleConstraintViolationShouldReturnBadRequest() {
        ConstraintViolationException ex = new ConstraintViolationException("invalid", null);

        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().get("error"));
    }

    @Test
    void handleNotFoundShouldReturn404() {
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(new NoSuchElementException("missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not Found", response.getBody().get("error"));
        assertEquals("missing", response.getBody().get("message"));
    }

    @Test
    void handleOrganizerKeyNotFoundShouldReturn404() {
        ResponseEntity<Map<String, Object>> response = handler.handleOrganizerKeyNotFound(new OrganizerKeyNotFoundException());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not Found", response.getBody().get("error"));
        assertEquals("Organizer key not found", response.getBody().get("message"));
    }

    @Test
    void handleOrganizerKeyAlreadyUsedShouldReturn410() {
        ResponseEntity<Map<String, Object>> response = handler.handleOrganizerKeyAlreadyUsed(new OrganizerKeyAlreadyUsedException());

        assertEquals(HttpStatus.GONE, response.getStatusCode());
        assertEquals("Gone", response.getBody().get("error"));
        assertEquals("Organizer key has already been used", response.getBody().get("message"));
    }

    @Test
    void handleOrganizerKeyExpiredShouldReturn401() {
        ResponseEntity<Map<String, Object>> response = handler.handleOrganizerKeyExpired(new OrganizerKeyExpiredException());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody().get("error"));
    }

    @Test
    void handleInvalidConfirmationTokenShouldReturn401() {
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidConfirmationToken(new InvalidConfirmationTokenException());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody().get("error"));
    }

    @Test
    void handleUsernameAlreadyTakenShouldReturn409() {
        ResponseEntity<Map<String, Object>> response = handler.handleRegistrationConflict(new UsernameAlreadyTakenException());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().get("error"));
    }

    @Test
    void handleEmailAlreadyRegisteredShouldReturn409() {
        ResponseEntity<Map<String, Object>> response = handler.handleRegistrationConflict(new EmailAlreadyRegisteredException());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().get("error"));
    }

    @Test
    void handleMaxUploadSizeShouldReturn413() {
        ResponseEntity<Map<String, Object>> response = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(1L));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, response.getStatusCode());
        assertEquals("Payload Too Large", response.getBody().get("error"));
        assertEquals(413, response.getBody().get("status"));
    }

    @Test
    void handleGenericExceptionShouldReturn500() {
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal Server Error", response.getBody().get("error"));
        assertEquals("An unexpected error occurred.", response.getBody().get("message"));
    }

    static class DummyController {
        public void create(@Valid EventDTO dto) {
        }
    }
}
