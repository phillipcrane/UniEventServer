package dk.unievent.web.repository;

import dk.unievent.web.model.PlaceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlaceRepositoryTests {

    @Autowired
    private PlaceRepository placeRepository;
    
    private PlaceEntity testPlace;
    
    @BeforeEach
    void setUp() {
        testPlace = new PlaceEntity();
        testPlace.setId("place-1");
        testPlace.setName("Test Place");
        testPlace.setStreet("123 Test Street");
        testPlace.setCity("Test City");
        testPlace.setZip("12345");
        testPlace.setCountry("Test Country");
        testPlace.setLatitude(55.6761);
        testPlace.setLongitude(12.5883);
        placeRepository.save(testPlace);

    }
    
    @Test
    void testSavePlace() {
        PlaceEntity place = new PlaceEntity();
        place.setId("place-2");
        place.setName("New Place");
        place.setCity("New City");
        place.setCountry("New Country");
        
        PlaceEntity savedPlace = placeRepository.save(place);
        
        assertNotNull(savedPlace);
        assertEquals("place-2", savedPlace.getId());
        assertEquals("New Place", savedPlace.getName());
        assertNotNull(savedPlace.getCreatedAt());
    }
    
    @Test
    void testFindPlaceById() {
        Optional<PlaceEntity> foundPlace = placeRepository.findById("place-1");
        
        assertTrue(foundPlace.isPresent());
        assertEquals("Test Place", foundPlace.get().getName());
        assertEquals("place-1", foundPlace.get().getId());
    }
    
    @Test
    void testFindPlaceByIdNotFound() {
        Optional<PlaceEntity> foundPlace = placeRepository.findById("non-existent");
        
        assertTrue(foundPlace.isEmpty());
    }
    
    @Test
    void testFindAllPlaces() {
        PlaceEntity place2 = new PlaceEntity();
        place2.setId("place-2");
        place2.setName("Second Place");
        place2.setCity("City 2");
        place2.setCountry("Country 2");
        placeRepository.save(place2);

        
        List<PlaceEntity> places = placeRepository.findAll();
        
        assertEquals(2, places.size());
    }
    
    @Test
    void testFindByCity() {
        PlaceEntity place2 = new PlaceEntity();
        place2.setId("place-2");
        place2.setName("Another Place");
        place2.setCity("Test City");
        place2.setCountry("Different Country");
        placeRepository.save(place2);
        
        PlaceEntity place3 = new PlaceEntity();
        place3.setId("place-3");
        place3.setName("Different City Place");
        place3.setCity("Different City");
        place3.setCountry("Test Country");
        placeRepository.save(place3);

        
        List<PlaceEntity> testCityPlaces = placeRepository.findByCity("Test City");
        
        assertEquals(2, testCityPlaces.size());
        assertTrue(testCityPlaces.stream().allMatch(p -> "Test City".equals(p.getCity())));
    }
    
    @Test
    void testFindByCountry() {
        PlaceEntity place2 = new PlaceEntity();
        place2.setId("place-2");
        place2.setName("Another Place");
        place2.setCity("Different City");
        place2.setCountry("Test Country");
        placeRepository.save(place2);
        
        PlaceEntity place3 = new PlaceEntity();
        place3.setId("place-3");
        place3.setName("Different Country Place");
        place3.setCity("City 3");
        place3.setCountry("Different Country");
        placeRepository.save(place3);

        
        List<PlaceEntity> testCountryPlaces = placeRepository.findByCountry("Test Country");
        
        assertEquals(2, testCountryPlaces.size());
        assertTrue(testCountryPlaces.stream().allMatch(p -> "Test Country".equals(p.getCountry())));
    }
    
    @Test
    void testFindByCityAndCountry() {
        PlaceEntity place2 = new PlaceEntity();
        place2.setId("place-2");
        place2.setName("Same City Different Country");
        place2.setCity("Test City");
        place2.setCountry("Different Country");
        placeRepository.save(place2);
        
        PlaceEntity place3 = new PlaceEntity();
        place3.setId("place-3");
        place3.setName("Different City Same Country");
        place3.setCity("Different City");
        place3.setCountry("Test Country");
        placeRepository.save(place3);

        
        List<PlaceEntity> places = placeRepository.findByCityAndCountry("Test City", "Test Country");
        
        assertEquals(1, places.size());
        assertEquals("place-1", places.get(0).getId());
    }
    
    @Test
    void testFindByNameIgnoreCase() {
        PlaceEntity place2 = new PlaceEntity();
        place2.setId("place-2");
        place2.setName("test place");
        place2.setCity("City 2");
        place2.setCountry("Country 2");
        placeRepository.save(place2);
        
        PlaceEntity place3 = new PlaceEntity();
        place3.setId("place-3");
        place3.setName("TEST PLACE");
        place3.setCity("City 3");
        place3.setCountry("Country 3");
        placeRepository.save(place3);

        
        List<PlaceEntity> places = placeRepository.findByNameIgnoreCase("test place");
        
        assertEquals(3, places.size());
        assertTrue(places.stream()
            .allMatch(p -> p.getName().equalsIgnoreCase("test place")));
    }
    
    @Test
    void testUpdatePlace() {
        PlaceEntity place = placeRepository.findById("place-1").orElseThrow();
        LocalDateTime originalUpdatedAt = place.getUpdatedAt();
        
        place.setName("Updated Place");
        place.setCity("Updated City");
        place.setLatitude(57.0);
        place.setLongitude(13.0);
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        placeRepository.save(place);

        
        PlaceEntity updatedPlace = placeRepository.findById("place-1").orElseThrow();
        
        assertEquals("Updated Place", updatedPlace.getName());
        assertEquals("Updated City", updatedPlace.getCity());
        assertEquals(57.0, updatedPlace.getLatitude());
        assertEquals(13.0, updatedPlace.getLongitude());
        assertTrue(updatedPlace.getUpdatedAt().isAfter(originalUpdatedAt) || 
                   updatedPlace.getUpdatedAt().isEqual(originalUpdatedAt));
    }
    
    @Test
    void testDeletePlace() {
        placeRepository.deleteById("place-1");

        
        Optional<PlaceEntity> deletedPlace = placeRepository.findById("place-1");
        
        assertTrue(deletedPlace.isEmpty());
    }
    
    @Test
    void testPlaceLocationFields() {
        PlaceEntity place = new PlaceEntity();
        place.setId("place-location");
        place.setName("Location Test");
        place.setStreet("456 Location St");
        place.setCity("Location City");
        place.setZip("67890");
        place.setCountry("Location Country");
        place.setLatitude(48.8566);
        place.setLongitude(2.3522);
        
        PlaceEntity savedPlace = placeRepository.save(place);

        
        PlaceEntity retrieved = placeRepository.findById("place-location").orElseThrow();
        
        assertEquals("456 Location St", retrieved.getStreet());
        assertEquals("Location City", retrieved.getCity());
        assertEquals("67890", retrieved.getZip());
        assertEquals("Location Country", retrieved.getCountry());
        assertEquals(48.8566, retrieved.getLatitude());
        assertEquals(2.3522, retrieved.getLongitude());
    }
    
    @Test
    void testPlaceWithMinimalData() {
        PlaceEntity place = new PlaceEntity();
        place.setId("place-minimal");
        place.setName("Minimal Place");
        // All other fields are null
        
        PlaceEntity savedPlace = placeRepository.save(place);

        
        PlaceEntity retrieved = placeRepository.findById("place-minimal").orElseThrow();
        
        assertEquals("Minimal Place", retrieved.getName());
        assertNull(retrieved.getCity());
        assertNull(retrieved.getCountry());
        assertNull(retrieved.getLatitude());
        assertNull(retrieved.getLongitude());
    }
    
    @Test
    void testFindByCityEmptyResult() {
        List<PlaceEntity> places = placeRepository.findByCity("Non-existent City");
        
        assertTrue(places.isEmpty());
    }
}

