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
 * Validation tests for LocationDTO
 * Tests all @Valid annotations and constraints
 */
@DisplayName("LocationDTO Validation Tests")
class LocationDTOValidationTests {
    
    private Validator validator;
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
    }
    
    // ============= Street Tests =============
    
    @Test
    @DisplayName("Valid LocationDTO passes validation")
    void testValidLocationDTO() {
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Street cannot be null")
    void testStreetNull() {
        locationDTO.setStreet(null);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("street")));
    }
    
    @Test
    @DisplayName("Street cannot be blank")
    void testStreetBlank() {
        locationDTO.setStreet("   ");
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("street")));
    }
    
    @Test
    @DisplayName("Street cannot exceed 255 characters")
    void testStreetTooLong() {
        locationDTO.setStreet("a".repeat(256));
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("street")));
    }
    
    @Test
    @DisplayName("Street with exactly 255 characters is valid")
    void testStreetMaxLength() {
        locationDTO.setStreet("a".repeat(255));
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    // ============= City Tests =============
    
    @Test
    @DisplayName("City cannot be null")
    void testCityNull() {
        locationDTO.setCity(null);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("city")));
    }
    
    @Test
    @DisplayName("City cannot be blank")
    void testCityBlank() {
        locationDTO.setCity("   ");
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("city")));
    }
    
    @Test
    @DisplayName("City cannot exceed 100 characters")
    void testCityTooLong() {
        locationDTO.setCity("a".repeat(101));
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("city")));
    }
    
    @Test
    @DisplayName("City with exactly 100 characters is valid")
    void testCityMaxLength() {
        locationDTO.setCity("a".repeat(100));
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    // ============= Zip Tests =============
    
    @Test
    @DisplayName("Zip cannot be null")
    void testZipNull() {
        locationDTO.setZip(null);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("zip")));
    }
    
    @Test
    @DisplayName("Zip cannot be blank")
    void testZipBlank() {
        locationDTO.setZip("   ");
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("zip")));
    }
    
    @Test
    @DisplayName("Zip must be 4-10 digits")
    void testZipTooShort() {
        locationDTO.setZip("123");
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("zip")));
    }
    
    @Test
    @DisplayName("Zip must be 4-10 digits (not too long)")
    void testZipTooLong() {
        locationDTO.setZip("12345678901");
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("zip")));
    }
    
    @Test
    @DisplayName("Zip with exactly 4 digits is valid")
    void testZipMin() {
        locationDTO.setZip("1234");
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Zip with exactly 10 digits is valid")
    void testZipMax() {
        locationDTO.setZip("1234567890");
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Zip cannot contain letters")
    void testZipWithLetters() {
        locationDTO.setZip("123a");
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("zip")));
    }
    
    // ============= Country Tests =============
    
    @Test
    @DisplayName("Country cannot be null")
    void testCountryNull() {
        locationDTO.setCountry(null);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("country")));
    }
    
    @Test
    @DisplayName("Country cannot be blank")
    void testCountryBlank() {
        locationDTO.setCountry("   ");
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("country")));
    }
    
    @Test
    @DisplayName("Country cannot exceed 100 characters")
    void testCountryTooLong() {
        locationDTO.setCountry("a".repeat(101));
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("country")));
    }
    
    @Test
    @DisplayName("Country with exactly 100 characters is valid")
    void testCountryMaxLength() {
        locationDTO.setCountry("a".repeat(100));
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    // ============= Latitude Tests =============
    
    @Test
    @DisplayName("Latitude cannot be null")
    void testLatitudeNull() {
        locationDTO.setLatitude(null);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("latitude")));
    }
    
    @Test
    @DisplayName("Latitude must be between -90 and 90")
    void testLatitudeBelow() {
        locationDTO.setLatitude(-91.0);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("latitude")));
    }
    
    @Test
    @DisplayName("Latitude must be between -90 and 90")
    void testLatitudeAbove() {
        locationDTO.setLatitude(91.0);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("latitude")));
    }
    
    @Test
    @DisplayName("Latitude at exactly -90 is valid")
    void testLatitudeMin() {
        locationDTO.setLatitude(-90.0);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Latitude at exactly 90 is valid")
    void testLatitudeMax() {
        locationDTO.setLatitude(90.0);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Latitude with decimal precision is valid")
    void testLatitudeDecimal() {
        locationDTO.setLatitude(55.7842);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    // ============= Longitude Tests =============
    
    @Test
    @DisplayName("Longitude cannot be null")
    void testLongitudeNull() {
        locationDTO.setLongitude(null);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("longitude")));
    }
    
    @Test
    @DisplayName("Longitude must be between -180 and 180")
    void testLongitudeBelow() {
        locationDTO.setLongitude(-181.0);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("longitude")));
    }
    
    @Test
    @DisplayName("Longitude must be between -180 and 180")
    void testLongitudeAbove() {
        locationDTO.setLongitude(181.0);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("longitude")));
    }
    
    @Test
    @DisplayName("Longitude at exactly -180 is valid")
    void testLongitudeMin() {
        locationDTO.setLongitude(-180.0);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Longitude at exactly 180 is valid")
    void testLongitudeMax() {
        locationDTO.setLongitude(180.0);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("Longitude with decimal precision is valid")
    void testLongitudeDecimal() {
        locationDTO.setLongitude(12.4933);
        Set<ConstraintViolation<LocationDTO>> violations = validator.validate(locationDTO);
        
        assertTrue(violations.isEmpty());
    }
}
