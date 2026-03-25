package dk.unievent.app.dto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dk.unievent.app.core.dto.PageDTO;

import org.junit.jupiter.api.DisplayName;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation tests for PageDTO
 * Tests all @Valid annotations and constraints
 */
@DisplayName("PageDTO Validation Tests")
class PageDTOValidationTests {
    
    private Validator validator;
    private PageDTO pageDTO;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Create a valid PageDTO for testing
        pageDTO = new PageDTO();
        pageDTO.setId("123456789");
        pageDTO.setName("S-huset");
        pageDTO.setUrl("https://facebook.com/123456789");
        pageDTO.setActive(true);
        pageDTO.setPictureId(456L);
    }
    
    // ============= Name Tests =============
    
    @Test
    @DisplayName("Valid PageDTO passes validation")
    void testValidPageDTO() {
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Name cannot be null")
    void testNameNull() {
        pageDTO.setName(null);
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
    
    @Test
    @DisplayName("Name cannot be blank")
    void testNameBlank() {
        pageDTO.setName("   ");
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
    
    @Test
    @DisplayName("Name cannot be empty")
    void testNameEmpty() {
        pageDTO.setName("");
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
    
    @Test
    @DisplayName("Name cannot exceed 255 characters")
    void testNameTooLong() {
        pageDTO.setName("a".repeat(256));
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
    
    @Test
    @DisplayName("Name with exactly 255 characters is valid")
    void testNameMaxLength() {
        pageDTO.setName("a".repeat(255));
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    // ============= URL Tests =============
    
    @Test
    @DisplayName("URL can be null")
    void testUrlNull() {
        pageDTO.setUrl(null);
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("URL must be valid Facebook URL")
    void testUrlInvalidFacebookUrl() {
        pageDTO.setUrl("https://google.com/page");
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("url")));
    }
    
    @Test
    @DisplayName("URL with facebook.com is valid")
    void testUrlValidFacebook() {
        pageDTO.setUrl("https://facebook.com/s-huset");
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("URL with facebook.com and numbers is valid")
    void testUrlValidFacebookNumbers() {
        pageDTO.setUrl("https://facebook.com/123456789");
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    // ============= PictureUrl Tests =============
    
    @Test
    @DisplayName("PictureUrl can be null")
    void testPictureUrlNull() {
        pageDTO.setPictureId(null);
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("PictureId can be negative")
    void testPictureIdNegative() {
        pageDTO.setPictureId(-1L);
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("PictureId with positive value is valid")
    void testPictureIdHttps() {
        pageDTO.setPictureId(1L);
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("PictureId with large value is valid")
    void testPictureIdHttp() {
        pageDTO.setPictureId(999999999L);
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    // ============= Active Tests =============
    
    @Test
    @DisplayName("Active can be null")
    void testActiveNull() {
        pageDTO.setActive(null);
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Active can be true")
    void testActiveTrue() {
        pageDTO.setActive(true);
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Active can be false")
    void testActiveFalse() {
        pageDTO.setActive(false);
        Set<ConstraintViolation<PageDTO>> violations = validator.validate(pageDTO);
        
        assertTrue(violations.isEmpty());
    }
}
