package dk.unievent.web.mapper;

import dk.unievent.web.dto.LocationDTO;
import dk.unievent.web.dto.PlaceDTO;
import dk.unievent.web.model.PlaceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlaceMapper
 * Tests conversion between PlaceEntity ↔ PlaceDTO
 * Includes nested LocationDTO conversions
 */
class PlaceMapperTests {
    
    private PlaceMapper placeMapper;
    
    private PlaceEntity testPlaceEntity;
    
    @BeforeEach
    void setUp() {
        placeMapper = new PlaceMapper();
        
        testPlaceEntity = new PlaceEntity();
        testPlaceEntity.setId("s-huset-lyngby");
        testPlaceEntity.setName("S-huset");
        testPlaceEntity.setStreet("Skjoldungsvej 100");
        testPlaceEntity.setCity("Lyngby");
        testPlaceEntity.setZip("2800");
        testPlaceEntity.setCountry("Denmark");
        testPlaceEntity.setLatitude(55.7842);
        testPlaceEntity.setLongitude(12.4933);
    }
    
    // ============= toDTO Tests =============
    
    @Test
    void testToDTOWithNull() {
        PlaceDTO result = placeMapper.toDTO(null);
        
        assertNull(result);
    }
    
    @Test
    void testToDTOWithFullEntity() {
        PlaceDTO result = placeMapper.toDTO(testPlaceEntity);
        
        assertNotNull(result);
        assertEquals("s-huset-lyngby", result.getId());
        assertEquals("S-huset", result.getName());
        
        assertNotNull(result.getLocation());
        assertEquals("Skjoldungsvej 100", result.getLocation().getStreet());
        assertEquals("Lyngby", result.getLocation().getCity());
        assertEquals("2800", result.getLocation().getZip());
        assertEquals("Denmark", result.getLocation().getCountry());
        assertEquals(55.7842, result.getLocation().getLatitude());
        assertEquals(12.4933, result.getLocation().getLongitude());
    }
    
    @Test
    void testToDTOWithMinimalEntity() {
        PlaceEntity entity = new PlaceEntity();
        entity.setId("place-minimal");
        entity.setName("Minimal Place");
        // All location fields are null
        
        PlaceDTO result = placeMapper.toDTO(entity);
        
        assertNotNull(result);
        assertEquals("place-minimal", result.getId());
        assertEquals("Minimal Place", result.getName());
        
        assertNotNull(result.getLocation());
        assertNull(result.getLocation().getStreet());
        assertNull(result.getLocation().getCity());
        assertNull(result.getLocation().getZip());
        assertNull(result.getLocation().getCountry());
        assertNull(result.getLocation().getLatitude());
        assertNull(result.getLocation().getLongitude());
    }
    
    @Test
    void testToDTOLocationMapping() {
        PlaceDTO result = placeMapper.toDTO(testPlaceEntity);
        
        LocationDTO location = result.getLocation();
        assertNotNull(location);
        
        // Verify all location fields are mapped
        assertEquals(testPlaceEntity.getStreet(), location.getStreet());
        assertEquals(testPlaceEntity.getCity(), location.getCity());
        assertEquals(testPlaceEntity.getZip(), location.getZip());
        assertEquals(testPlaceEntity.getCountry(), location.getCountry());
        assertEquals(testPlaceEntity.getLatitude(), location.getLatitude());
        assertEquals(testPlaceEntity.getLongitude(), location.getLongitude());
    }
    
    @Test
    void testToDTOWithNullLocationFields() {
        testPlaceEntity.setStreet(null);
        testPlaceEntity.setCity(null);
        testPlaceEntity.setZip(null);
        testPlaceEntity.setCountry(null);
        testPlaceEntity.setLatitude(null);
        testPlaceEntity.setLongitude(null);
        
        PlaceDTO result = placeMapper.toDTO(testPlaceEntity);
        
        assertNotNull(result);
        assertNotNull(result.getLocation());
        assertNull(result.getLocation().getStreet());
        assertNull(result.getLocation().getCity());
        assertNull(result.getLocation().getZip());
        assertNull(result.getLocation().getCountry());
        assertNull(result.getLocation().getLatitude());
        assertNull(result.getLocation().getLongitude());
    }
    
    @Test
    void testToDTOPartialLocationFields() {
        testPlaceEntity.setStreet("Main Street");
        testPlaceEntity.setCity("Copenhagen");
        testPlaceEntity.setZip(null);
        testPlaceEntity.setCountry("Denmark");
        testPlaceEntity.setLatitude(55.6761);
        testPlaceEntity.setLongitude(null);
        
        PlaceDTO result = placeMapper.toDTO(testPlaceEntity);
        
        LocationDTO location = result.getLocation();
        assertEquals("Main Street", location.getStreet());
        assertEquals("Copenhagen", location.getCity());
        assertNull(location.getZip());
        assertEquals("Denmark", location.getCountry());
        assertEquals(55.6761, location.getLatitude());
        assertNull(location.getLongitude());
    }
    
    @Test
    void testToDTOCoordinatesMapping() {
        PlaceDTO result = placeMapper.toDTO(testPlaceEntity);
        
        assertEquals(55.7842, result.getLocation().getLatitude());
        assertEquals(12.4933, result.getLocation().getLongitude());
    }
    
    // ============= toEntity Tests =============
    
    @Test
    void testToEntityWithNull() {
        PlaceEntity result = placeMapper.toEntity(null);
        
        assertNull(result);
    }
    
    @Test
    void testToEntityWithFullDTO() {
        PlaceDTO dto = new PlaceDTO();
        dto.setId("place-new");
        dto.setName("New Venue");
        
        LocationDTO location = new LocationDTO();
        location.setStreet("New Street 42");
        location.setCity("New City");
        location.setZip("1234");
        location.setCountry("New Country");
        location.setLatitude(50.1234);
        location.setLongitude(14.5678);
        dto.setLocation(location);
        
        PlaceEntity result = placeMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals("place-new", result.getId());
        assertEquals("New Venue", result.getName());
        assertEquals("New Street 42", result.getStreet());
        assertEquals("New City", result.getCity());
        assertEquals("1234", result.getZip());
        assertEquals("New Country", result.getCountry());
        assertEquals(50.1234, result.getLatitude());
        assertEquals(14.5678, result.getLongitude());
    }
    
    @Test
    void testToEntityWithNullLocation() {
        PlaceDTO dto = new PlaceDTO();
        dto.setId("place-no-location");
        dto.setName("No Location Place");
        dto.setLocation(null);
        
        PlaceEntity result = placeMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals("place-no-location", result.getId());
        assertEquals("No Location Place", result.getName());
        assertNull(result.getStreet());
        assertNull(result.getCity());
        assertNull(result.getZip());
        assertNull(result.getCountry());
        assertNull(result.getLatitude());
        assertNull(result.getLongitude());
    }
    
    @Test
    void testToEntityWithPartialLocationDTO() {
        PlaceDTO dto = new PlaceDTO();
        dto.setId("place-partial");
        dto.setName("Partial Location");
        
        LocationDTO location = new LocationDTO();
        location.setCity("Prague");
        location.setCountry("Czech Republic");
        location.setLatitude(50.0755);
        // street, zip, longitude are null
        dto.setLocation(location);
        
        PlaceEntity result = placeMapper.toEntity(dto);
        
        assertNull(result.getStreet());
        assertEquals("Prague", result.getCity());
        assertNull(result.getZip());
        assertEquals("Czech Republic", result.getCountry());
        assertEquals(50.0755, result.getLatitude());
        assertNull(result.getLongitude());
    }
    
    @Test
    void testToEntityWithEmptyLocationDTO() {
        PlaceDTO dto = new PlaceDTO();
        dto.setId("place-empty-location");
        dto.setName("Empty Location");
        dto.setLocation(new LocationDTO()); // All fields null
        
        PlaceEntity result = placeMapper.toEntity(dto);
        
        assertNotNull(result);
        assertNull(result.getStreet());
        assertNull(result.getCity());
        assertNull(result.getZip());
        assertNull(result.getCountry());
        assertNull(result.getLatitude());
        assertNull(result.getLongitude());
    }
    
    @Test
    void testDTOtoEntityRoundTrip() {
        // Entity -> DTO -> Entity should preserve core fields
        PlaceDTO dto = placeMapper.toDTO(testPlaceEntity);
        PlaceEntity result = placeMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals(testPlaceEntity.getId(), result.getId());
        assertEquals(testPlaceEntity.getName(), result.getName());
        assertEquals(testPlaceEntity.getStreet(), result.getStreet());
        assertEquals(testPlaceEntity.getCity(), result.getCity());
        assertEquals(testPlaceEntity.getZip(), result.getZip());
        assertEquals(testPlaceEntity.getCountry(), result.getCountry());
        assertEquals(testPlaceEntity.getLatitude(), result.getLatitude());
        assertEquals(testPlaceEntity.getLongitude(), result.getLongitude());
    }
    
    @Test
    void testToEntityWithMinimalDTO() {
        PlaceDTO dto = new PlaceDTO();
        dto.setId("minimal-place");
        dto.setName("Minimal");
        
        PlaceEntity result = placeMapper.toEntity(dto);
        
        assertNotNull(result);
        assertEquals("minimal-place", result.getId());
        assertEquals("Minimal", result.getName());
        assertNull(result.getStreet());
        assertNull(result.getCity());
    }
}
