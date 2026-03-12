package dk.unievent.web.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation tests for PlaceDTO
 * Tests all @Valid annotations and constraints
 */
@DisplayName("PlaceDTO Validation Tests")
class PlaceDTOValidationTests {
    
    private Validator validator;
    private PlaceDTO placeDTO;
    private LocationDTO locationDTO;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Create valid LocationDTO
        locationDTO = new LocationDTO();
        locationDTO.setStreet("Skjoldungsvej 100");
        locationDTO.setCity("Lyngby");
        locationDTO.setZip("2800");
        locationDTO.setCountry("Denmark");
        locationDTO.setLatitude(55.7842);
        locationDTO.setLongitude(12.4933);
        
        // Create valid PlaceDTO
        placeDTO = new PlaceDTO();
        placeDTO.setId("s-huset-lyngby");
        placeDTO.setName("S-huset");
        placeDTO.setLocation(locationDTO);
    }
    
    // ============= Name Tests =============
    
    @Test
    @DisplayName("Valid PlaceDTO passes validation")
    void testValidPlaceDTO() {
        Set<ConstraintViolation<PlaceDTO>> violations = validator.validate(placeDTO);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Name cannot be null")
    void testNameNull() {
        placeDTO.setName(null);
        Set<ConstraintViolation<PlaceDTO>> violations = validator.validate(placeDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
    
    @Test
    @DisplayName("Name cannot be blank")
    void testNameBlank() {
        placeDTO.setName("   ");
        Set<ConstraintViolation<PlaceDTO>> violations = validator.validate(placeDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
    
    @Test
    @DisplayName("Name cannot be empty")
    void testNameEmpty() {
        placeDTO.setName("");
        Set<ConstraintViolation<PlaceDTO>> violations = validator.validate(placeDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
    
    @Test
    @DisplayName("Name cannot exceed 255 characters")
    void testNameTooLong() {
        placeDTO.setName("a".repeat(256));
        Set<ConstraintViolation<PlaceDTO>> violations = validator.validate(placeDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
    
    @Test
    @DisplayName("Name with exactly 255 characters is valid")
    void testNameMaxLength() {
        placeDTO.setName("a".repeat(255));
        Set<ConstraintViolation<PlaceDTO>> violations = validator.validate(placeDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    // ============= Location Tests =============
    
    @Test
    @DisplayName("Location can be null")
    void testLocationNull() {
        placeDTO.setLocation(null);
        Set<ConstraintViolation<PlaceDTO>> violations = validator.validate(placeDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Invalid nested LocationDTO causes violation")
    void testInvalidNestedLocation() {
        LocationDTO invalidLocation = new LocationDTO();
        invalidLocation.setStreet(null); // Required field
        placeDTO.setLocation(invalidLocation);
        
        Set<ConstraintViolation<PlaceDTO>> violations = validator.validate(placeDTO);
        
        assertFalse(violations.isEmpty());
    }
}
