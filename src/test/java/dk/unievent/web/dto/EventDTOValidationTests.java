package dk.unievent.web.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation tests for EventDTO
 * Tests all @Valid annotations and constraints
 */
@DisplayName("EventDTO Validation Tests")
class EventDTOValidationTests {
    
    private Validator validator;
    private EventDTO eventDTO;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Create a valid EventDTO for testing
        eventDTO = new EventDTO();
        eventDTO.setPageId("page-123");
        eventDTO.setTitle("Valid Event Title");
        eventDTO.setDescription("This is a valid event description");
        eventDTO.setStartTime(LocalDateTime.of(2026, 3, 20, 18, 0));
        eventDTO.setEndTime(LocalDateTime.of(2026, 3, 20, 20, 0));
        eventDTO.setCoverImageId(1L);
        eventDTO.setEventURL("https://example.com/event");
    }
    
    // ============= PageId Tests =============
    
    @Test
    @DisplayName("Valid EventDTO passes validation")
    void testValidEventDTO() {
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("PageId cannot be null")
    void testPageIdNull() {
        eventDTO.setPageId(null);
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("pageId")));
    }
    
    @Test
    @DisplayName("PageId cannot be blank")
    void testPageIdBlank() {
        eventDTO.setPageId("   ");
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("pageId")));
    }
    
    @Test
    @DisplayName("PageId cannot be empty string")
    void testPageIdEmpty() {
        eventDTO.setPageId("");
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("pageId")));
    }
    
    // ============= Title Tests =============
    
    @Test
    @DisplayName("Title cannot be null")
    void testTitleNull() {
        eventDTO.setTitle(null);
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }
    
    @Test
    @DisplayName("Title cannot be blank")
    void testTitleBlank() {
        eventDTO.setTitle("   ");
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }
    
    @Test
    @DisplayName("Title cannot exceed 255 characters")
    void testTitleTooLong() {
        eventDTO.setTitle("a".repeat(256));
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }
    
    @Test
    @DisplayName("Title with exactly 255 characters is valid")
    void testTitleMaxLength() {
        eventDTO.setTitle("a".repeat(255));
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    // ============= Description Tests =============
    
    @Test
    @DisplayName("Description can be null")
    void testDescriptionNull() {
        eventDTO.setDescription(null);
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Description cannot exceed 2000 characters")
    void testDescriptionTooLong() {
        eventDTO.setDescription("a".repeat(2001));
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("description")));
    }
    
    @Test
    @DisplayName("Description with exactly 2000 characters is valid")
    void testDescriptionMaxLength() {
        eventDTO.setDescription("a".repeat(2000));
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    // ============= StartTime Tests =============
    
    @Test
    @DisplayName("StartTime cannot be null")
    void testStartTimeNull() {
        eventDTO.setStartTime(null);
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("startTime")));
    }
    
    // ============= EndTime Tests =============
    
    @Test
    @DisplayName("EndTime cannot be null")
    void testEndTimeNull() {
        eventDTO.setEndTime(null);
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("endTime")));
    }
    
    // ============= Cover Image ID Tests =============
    
    @Test
    @DisplayName("CoverImageId can be null")
    void testCoverImageIdNull() {
        eventDTO.setCoverImageId(null);
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("CoverImageId can be a positive long")
    void testCoverImageIdValid() {
        eventDTO.setCoverImageId(1L);
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("CoverImageId with large value is valid")
    void testCoverImageIdLargeValue() {
        eventDTO.setCoverImageId(999999999L);
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("EventURL must be valid URL")
    void testEventURLInvalid() {
        eventDTO.setEventURL("not-a-url");
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("eventURL")));
    }
    
    @Test
    @DisplayName("EventURL with https is valid")
    void testEventURLHttps() {
        eventDTO.setEventURL("https://facebook.com/events/123");
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    // ============= Nested PlaceDTO Tests =============
    
    @Test
    @DisplayName("Place can be null")
    void testPlaceNull() {
        eventDTO.setPlace(null);
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Invalid nested PlaceDTO causes violation")
    void testInvalidNestedPlace() {
        PlaceDTO invalidPlace = new PlaceDTO();
        invalidPlace.setName(null); // Required field
        eventDTO.setPlace(invalidPlace);
        
        Set<ConstraintViolation<EventDTO>> violations = validator.validate(eventDTO);
        
        assertFalse(violations.isEmpty());
    }
}
