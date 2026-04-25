package dk.unievent.app.api.handler;

import dk.unievent.app.infrastructure.exception.FacebookApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import dk.unievent.app.infrastructure.exception.EmailAlreadyRegisteredException;
import dk.unievent.app.infrastructure.exception.InvalidConfirmationTokenException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyAlreadyUsedException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyExpiredException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyNotFoundException;
import dk.unievent.app.infrastructure.exception.UnauthorizedTokenException;
import dk.unievent.app.infrastructure.exception.UsernameAlreadyTakenException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.security.core.AuthenticationException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Global exception handler for REST controllers
 * Handles validation errors and returns consistent error responses
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle validation errors from @Valid annotation
     * Returns a detailed error response with field-level error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        log.warn("Validation error: {}", ex.getBindingResult().getErrorCount());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Validation Failed");
        errorResponse.put("message", "Input validation failed. See 'errors' field for details.");
        
        Map<String, String> errors = new HashMap<>();
        
        // Extract all field validation errors
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
            log.debug("Field validation error - field: {}, message: {}", error.getField(), error.getDefaultMessage());
        });
        
        // Extract global validation errors
        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            errors.put(error.getObjectName(), error.getDefaultMessage());
            log.debug("Global validation error - object: {}, message: {}", error.getObjectName(), error.getDefaultMessage());
        });
        
        errorResponse.put("errors", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        log.warn("Bad request error: {}", ex.getClass().getSimpleName());
        log.debug("Exception message: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", "A resource with the same unique identifier already exists.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid credentials.");
    }

    @ExceptionHandler(UnauthorizedTokenException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedTokenException ex) {
        log.warn("Unauthorized token error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(OrganizerKeyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrganizerKeyNotFound(OrganizerKeyNotFoundException ex) {
        log.warn("Organizer key not found");
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(OrganizerKeyAlreadyUsedException.class)
    public ResponseEntity<Map<String, Object>> handleOrganizerKeyAlreadyUsed(OrganizerKeyAlreadyUsedException ex) {
        log.warn("Organizer key already used");
        return buildErrorResponse(HttpStatus.GONE, "Gone", ex.getMessage());
    }

    @ExceptionHandler(OrganizerKeyExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleOrganizerKeyExpired(OrganizerKeyExpiredException ex) {
        log.warn("Organizer key expired");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(InvalidConfirmationTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidConfirmationToken(InvalidConfirmationTokenException ex) {
        log.warn("Invalid confirmation token");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler({UsernameAlreadyTakenException.class, EmailAlreadyRegisteredException.class})
    public ResponseEntity<Map<String, Object>> handleRegistrationConflict(RuntimeException ex) {
        log.warn("Registration conflict: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded: {}", ex.getMessage());
        return buildErrorResponse(
            HttpStatus.CONTENT_TOO_LARGE,
                "Payload Too Large",
                "Uploaded file exceeds the maximum allowed size."
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex) {
        log.error("I/O error occurred: ", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "I/O operation failed."
        );
    }

    @ExceptionHandler(FacebookApiException.class)
    public ResponseEntity<Map<String, Object>> handleFacebookApiException(FacebookApiException ex) {
        log.error("Facebook API error: {} (status: {}, error type: {})",
            ex.getMessage(), ex.getStatusCode(), ex.getErrorType());

        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", "Facebook service is currently unavailable. Please try again later.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: ", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred."
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return ResponseEntity.status(status).body(errorResponse);
    }
}
