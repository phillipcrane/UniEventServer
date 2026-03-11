package dk.unievent.web.integration;

import dk.unievent.web.dto.LocationDTO;
import dk.unievent.web.dto.PlaceDTO;
import dk.unievent.web.model.PlaceEntity;
import dk.unievent.web.repository.PlaceRepository;
import dk.unievent.web.service.PlaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Place (Venue) operations
 * Tests full stack: Database ↔ Repository ↔ Service ↔ Mapper ↔ DTO
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlaceIntegrationTests {
    
    @Autowired
    private PlaceRepository placeRepository;
    
    @Autowired
    private PlaceService placeService;
    
    // ============= Create Tests =============
    
    @Test
    void testCreatePlaceAndRetrieveAsDTO() {
        PlaceDTO createDTO = createPlaceDTO("venue-1", "New Venue", "Main St", "Copenhagen");
        
        PlaceDTO created = placeService.createPlace(createDTO);
        
        assertNotNull(created);
        assertEquals("venue-1", created.getId());
        assertEquals("New Venue", created.getName());
        assertEquals("Copenhagen", created.getLocation().getCity());
    }
    
    @Test
    void testPlacePersistenceInDatabase() {
        PlaceDTO createDTO = createPlaceDTO("persist-venue", "Persist Venue", "Street 1", "Aarhus");
        
        placeService.createPlace(createDTO);
        
        // Verify in database
        Optional<PlaceEntity> dbEntity = placeRepository.findById("persist-venue");
        assertTrue(dbEntity.isPresent());
        assertEquals("Persist Venue", dbEntity.get().getName());
        assertEquals("Street 1", dbEntity.get().getStreet());
        assertEquals("Aarhus", dbEntity.get().getCity());
    }
    
    // ============= Retrieve Tests =============
    
    @Test
    void testGetPlaceById() {
        PlaceDTO created = createTestPlace("place-find", "Find Me Venue", "Find St", "Odense");
        
        PlaceDTO found = placeService.getPlaceById("place-find");
        
        assertNotNull(found);
        assertEquals("Find Me Venue", found.getName());
        assertEquals("Odense", found.getLocation().getCity());
    }
    
    @Test
    void testGetPlaceByIdNotFoundReturnsNull() {
        PlaceDTO notFound = placeService.getPlaceById("nonexistent");
        
        assertNull(notFound);
    }
    
    @Test
    void testGetPlacesByCity() {
        createTestPlace("place-cph-1", "Venue CPH 1", "Street 1", "Copenhagen");
        createTestPlace("place-cph-2", "Venue CPH 2", "Street 2", "Copenhagen");
        createTestPlace("place-aarhus", "Venue Aarhus", "Street 3", "Aarhus");
        
        List<PlaceDTO> cphPlaces = placeService.getPlacesByCity("Copenhagen");
        List<PlaceDTO> aarhusPlaces = placeService.getPlacesByCity("Aarhus");
        
        assertEquals(2, cphPlaces.size());
        assertEquals(1, aarhusPlaces.size());
        assertTrue(cphPlaces.stream().allMatch(p -> "Copenhagen".equals(p.getLocation().getCity())));
    }
    
    @Test
    void testGetPlacesByCountry() {
        createTestPlace("place-dk-1", "Venue Denmark 1", "Street 1", "Copenhagen", "Denmark");
        createTestPlace("place-dk-2", "Venue Denmark 2", "Street 2", "Aarhus", "Denmark");
        createTestPlace("place-se", "Venue Sweden", "Street 3", "Stockholm", "Sweden");
        
        List<PlaceDTO> danishPlaces = placeService.getPlacesByCountry("Denmark");
        List<PlaceDTO> swedishPlaces = placeService.getPlacesByCountry("Sweden");
        
        assertEquals(2, danishPlaces.size());
        assertEquals(1, swedishPlaces.size());
        assertTrue(danishPlaces.stream().allMatch(p -> "Denmark".equals(p.getLocation().getCountry())));
    }
    
    @Test
    void testGetPlacesByCityAndCountry() {
        createTestPlace("place-cph-dk", "Venue Copenhagen", "Street 1", "Copenhagen", "Denmark");
        createTestPlace("place-cph-se", "Venue Stockholm", "Street 2", "Stockholm", "Sweden");
        
        List<PlaceDTO> cphDkPlaces = placeService.getPlacesByCityAndCountry("Copenhagen", "Denmark");
        List<PlaceDTO> stockholmPlaces = placeService.getPlacesByCityAndCountry("Stockholm", "Sweden");
        List<PlaceDTO> noMatch = placeService.getPlacesByCityAndCountry("Paris", "France");
        
        assertEquals(1, cphDkPlaces.size());
        assertEquals(1, stockholmPlaces.size());
        assertEquals(0, noMatch.size());
    }
    
    @Test
    void testSearchPlacesByName() {
        createTestPlace("place-1", "S-huset", "Street 1", "Copenhagen");
        createTestPlace("place-2", "Slotsholmen", "Street 2", "Copenhagen");
        createTestPlace("place-3", "Pumpehuset", "Street 3", "Copenhagen");
        
        List<PlaceDTO> searchResults = placeService.searchByName("s-huset");
        
        assertEquals(1, searchResults.size());
        assertEquals("S-huset", searchResults.get(0).getName());
    }
    
    @Test
    void testSearchPlacesByNamePartial() {
        createTestPlace("place-1", "S-huset", "Street 1", "Copenhagen");
        createTestPlace("place-2", "S-huset", "Street 2", "Copenhagen");
        createTestPlace("place-3", "Pumpehuset", "Street 3", "Copenhagen");
        
        List<PlaceDTO> searchResults = placeService.searchByName("S-huset");
        
        assertEquals(2, searchResults.size());
    }
    
    // ============= Update Tests =============
    
    @Test
    void testUpdatePlaceThroughService() {
        PlaceDTO created = createTestPlace("place-update", "Original Name", "Street 1", "Copenhagen");
        
        PlaceDTO updateDTO = new PlaceDTO();
        updateDTO.setId("place-update");
        updateDTO.setName("Updated Name");
        
        LocationDTO updateLocation = new LocationDTO();
        updateLocation.setStreet("Updated Street");
        updateLocation.setCity("Aarhus");
        updateLocation.setCountry("Denmark");
        updateDTO.setLocation(updateLocation);
        
        PlaceDTO updated = placeService.updatePlace("place-update", updateDTO);
        
        assertNotNull(updated);
        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated Street", updated.getLocation().getStreet());
        assertEquals("Aarhus", updated.getLocation().getCity());
    }
    
    @Test
    void testUpdatePlaceNotFoundReturnsNull() {
        PlaceDTO updateDTO = new PlaceDTO();
        updateDTO.setName("New Name");
        
        PlaceDTO result = placeService.updatePlace("nonexistent", updateDTO);
        
        assertNull(result);
    }
    
    @Test
    void testUpdatePlacePersistsToDatabase() {
        createTestPlace("place-persist", "Before Update", "Street 1", "Copenhagen");
        
        PlaceDTO updateDTO = new PlaceDTO();
        updateDTO.setName("After Update");
        updateDTO.setLocation(new LocationDTO());
        
        placeService.updatePlace("place-persist", updateDTO);
        
        // Verify in database
        Optional<PlaceEntity> dbEntity = placeRepository.findById("place-persist");
        assertTrue(dbEntity.isPresent());
        assertEquals("After Update", dbEntity.get().getName());
    }
    
    // ============= Delete Tests =============
    
    @Test
    void testDeletePlaceThroughService() {
        createTestPlace("place-delete", "Delete Me", "Street 1", "Copenhagen");
        
        boolean deleted = placeService.deletePlace("place-delete");
        
        assertTrue(deleted);
        // Verify deleted from database
        assertFalse(placeRepository.findById("place-delete").isPresent());
    }
    
    @Test
    void testDeletePlaceNotFoundReturnsFalse() {
        boolean deleted = placeService.deletePlace("nonexistent");
        
        assertFalse(deleted);
    }
    
    // ============= Location Field Tests =============
    
    @Test
    void testPlaceWithCompleteLocationFields() {
        PlaceDTO dto = new PlaceDTO();
        dto.setId("place-complete");
        dto.setName("Complete Venue");
        
        LocationDTO location = new LocationDTO();
        location.setStreet("Main Street 123");
        location.setCity("Copenhagen");
        location.setZip("2100");
        location.setCountry("Denmark");
        location.setLatitude(55.6761);
        location.setLongitude(12.5883);
        dto.setLocation(location);
        
        PlaceDTO created = placeService.createPlace(dto);
        PlaceDTO retrieved = placeService.getPlaceById("place-complete");
        
        assertNotNull(retrieved);
        assertEquals("Main Street 123", retrieved.getLocation().getStreet());
        assertEquals("2100", retrieved.getLocation().getZip());
        assertEquals(55.6761, retrieved.getLocation().getLatitude());
        assertEquals(12.5883, retrieved.getLocation().getLongitude());
    }
    
    // ============= Helper Methods =============
    
    private PlaceDTO createPlaceDTO(String id, String name, String street, String city) {
        PlaceDTO dto = new PlaceDTO();
        dto.setId(id);
        dto.setName(name);
        
        LocationDTO location = new LocationDTO();
        location.setStreet(street);
        location.setCity(city);
        location.setCountry("Denmark");
        dto.setLocation(location);
        
        return dto;
    }
    
    private PlaceDTO createPlaceDTO(String id, String name, String street, String city, String country) {
        PlaceDTO dto = createPlaceDTO(id, name, street, city);
        dto.getLocation().setCountry(country);
        return dto;
    }
    
    private PlaceDTO createTestPlace(String id, String name, String street, String city) {
        return placeService.createPlace(createPlaceDTO(id, name, street, city));
    }
    
    private PlaceDTO createTestPlace(String id, String name, String street, String city, String country) {
        return placeService.createPlace(createPlaceDTO(id, name, street, city, country));
    }
}
