package dk.unievent.app.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.unievent.app.core.dto.LocationDTO;
import dk.unievent.app.core.dto.PlaceDTO;
import dk.unievent.app.core.mapper.PlaceMapper;
import dk.unievent.app.core.service.PlaceService;
import dk.unievent.app.mysql.model.PlaceEntity;
import dk.unievent.app.mysql.repository.PlaceRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTests {
    
    @Mock
    private PlaceRepository placeRepository;
    
    @Mock
    private PlaceMapper placeMapper;
    
    @InjectMocks
    private PlaceService placeService;
    
    private PlaceEntity testPlaceEntity;
    private PlaceDTO testPlaceDTO;
    
    @BeforeEach
    void setUp() {
        testPlaceEntity = PlaceEntity.builder()
                .id("place-1")
                .name("Test Place")
                .city("Test City")
                .country("Test Country")
                .latitude(55.6761)
                .longitude(12.5883)
                .build();
        
        LocationDTO location = new LocationDTO();
        location.setCity("Test City");
        location.setCountry("Test Country");
        location.setStreet("123 Test St");
        location.setZip("12345");
        location.setLatitude(55.6761);
        location.setLongitude(12.5883);
        
        testPlaceDTO = new PlaceDTO();
        testPlaceDTO.setId("place-1");
        testPlaceDTO.setName("Test Place");
        testPlaceDTO.setLocation(location);
    }
    
    @Test
    void testGetPlaceById() {
        when(placeRepository.findById("place-1")).thenReturn(Optional.of(testPlaceEntity));
        when(placeMapper.toDTO(testPlaceEntity)).thenReturn(testPlaceDTO);
        
        PlaceDTO result = placeService.getPlaceById("place-1");
        
        assertNotNull(result);
        assertEquals("place-1", result.getId());
        assertEquals("Test Place", result.getName());
        verify(placeRepository, times(1)).findById("place-1");
        verify(placeMapper, times(1)).toDTO(testPlaceEntity);
    }
    
    @Test
    void testGetPlaceByIdNotFound() {
        when(placeRepository.findById("non-existent")).thenReturn(Optional.empty());
        
        PlaceDTO result = placeService.getPlaceById("non-existent");
        
        assertNull(result);
    }
    
    @Test
    void testGetPlacesByCity() {
        List<PlaceEntity> cityPlaces = List.of(testPlaceEntity);
        when(placeRepository.findByCity("Test City")).thenReturn(cityPlaces);
        when(placeMapper.toDTO(testPlaceEntity)).thenReturn(testPlaceDTO);
        
        List<PlaceDTO> result = placeService.getPlacesByCity("Test City");
        
        assertEquals(1, result.size());
        assertEquals("place-1", result.get(0).getId());
        verify(placeRepository, times(1)).findByCity("Test City");
    }
    
    @Test
    void testGetPlacesByCityEmpty() {
        when(placeRepository.findByCity("Unknown City")).thenReturn(List.of());
        
        List<PlaceDTO> result = placeService.getPlacesByCity("Unknown City");
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetPlacesByCountry() {
        List<PlaceEntity> countryPlaces = List.of(testPlaceEntity);
        when(placeRepository.findByCountry("Test Country")).thenReturn(countryPlaces);
        when(placeMapper.toDTO(testPlaceEntity)).thenReturn(testPlaceDTO);
        
        List<PlaceDTO> result = placeService.getPlacesByCountry("Test Country");
        
        assertEquals(1, result.size());
        verify(placeRepository, times(1)).findByCountry("Test Country");
    }
    
    @Test
    void testGetPlacesByCountryEmpty() {
        when(placeRepository.findByCountry("Unknown Country")).thenReturn(List.of());
        
        List<PlaceDTO> result = placeService.getPlacesByCountry("Unknown Country");
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetPlacesByCityAndCountry() {
        List<PlaceEntity> places = List.of(testPlaceEntity);
        when(placeRepository.findByCityAndCountry("Test City", "Test Country"))
            .thenReturn(places);
        when(placeMapper.toDTO(testPlaceEntity)).thenReturn(testPlaceDTO);
        
        List<PlaceDTO> result = placeService.getPlacesByCityAndCountry("Test City", "Test Country");
        
        assertEquals(1, result.size());
        verify(placeRepository, times(1)).findByCityAndCountry("Test City", "Test Country");
    }
    
    @Test
    void testGetPlacesByCityAndCountryEmpty() {
        when(placeRepository.findByCityAndCountry("Unknown", "Unknown"))
            .thenReturn(List.of());
        
        List<PlaceDTO> result = placeService.getPlacesByCityAndCountry("Unknown", "Unknown");
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testSearchByName() {
        List<PlaceEntity> searchResults = List.of(testPlaceEntity);
        when(placeRepository.findByNameIgnoreCase("test place"))
            .thenReturn(searchResults);
        when(placeMapper.toDTO(testPlaceEntity)).thenReturn(testPlaceDTO);
        
        List<PlaceDTO> result = placeService.searchByName("test place");
        
        assertEquals(1, result.size());
        verify(placeRepository, times(1)).findByNameIgnoreCase("test place");
    }
    
    @Test
    void testSearchByNameEmpty() {
        when(placeRepository.findByNameIgnoreCase("not-found"))
            .thenReturn(List.of());
        
        List<PlaceDTO> result = placeService.searchByName("not-found");
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testCreatePlace() {
        when(placeMapper.toEntity(testPlaceDTO)).thenReturn(testPlaceEntity);
        when(placeRepository.save(testPlaceEntity)).thenReturn(testPlaceEntity);
        when(placeMapper.toDTO(testPlaceEntity)).thenReturn(testPlaceDTO);
        
        PlaceDTO result = placeService.createPlace(testPlaceDTO);
        
        assertNotNull(result);
        assertEquals("place-1", result.getId());
        verify(placeMapper, times(1)).toEntity(testPlaceDTO);
        verify(placeRepository, times(1)).save(testPlaceEntity);
        verify(placeMapper, times(1)).toDTO(testPlaceEntity);
    }
    
    @Test
    void testUpdatePlace() {
        when(placeRepository.findById("place-1")).thenReturn(Optional.of(testPlaceEntity));
        when(placeRepository.save(any(PlaceEntity.class))).thenReturn(testPlaceEntity);
        when(placeMapper.toDTO(testPlaceEntity)).thenReturn(testPlaceDTO);
        
        PlaceDTO updateDTO = new PlaceDTO();
        updateDTO.setName("Updated Place");
        
        LocationDTO location = new LocationDTO();
        location.setCity("Updated City");
        location.setCountry("Updated Country");
        location.setStreet("456 Updated St");
        updateDTO.setLocation(location);
        
        PlaceDTO result = placeService.updatePlace("place-1", updateDTO);
        
        assertNotNull(result);
        assertEquals("place-1", result.getId());
        verify(placeRepository, times(1)).findById("place-1");
        verify(placeRepository, times(1)).save(any(PlaceEntity.class));
    }
    
    @Test
    void testUpdatePlaceNotFound() {
        when(placeRepository.findById("non-existent")).thenReturn(Optional.empty());
        
        PlaceDTO updateDTO = new PlaceDTO();
        updateDTO.setName("Updated");
        
        PlaceDTO result = placeService.updatePlace("non-existent", updateDTO);
        
        assertNull(result);
        verify(placeRepository, times(1)).findById("non-existent");
        verify(placeRepository, never()).save(any());
    }
    
    @Test
    void testDeletePlace() {
        when(placeRepository.existsById("place-1")).thenReturn(true);
        
        boolean result = placeService.deletePlace("place-1");
        
        assertTrue(result);
        verify(placeRepository, times(1)).existsById("place-1");
        verify(placeRepository, times(1)).deleteById("place-1");
    }
    
    @Test
    void testDeletePlaceNotFound() {
        when(placeRepository.existsById("non-existent")).thenReturn(false);
        
        boolean result = placeService.deletePlace("non-existent");
        
        assertFalse(result);
        verify(placeRepository, times(1)).existsById("non-existent");
        verify(placeRepository, never()).deleteById("non-existent");
    }
    
    @Test
    void testUpdatePlaceWithoutLocation() {
        when(placeRepository.findById("place-1")).thenReturn(Optional.of(testPlaceEntity));
        when(placeRepository.save(any(PlaceEntity.class))).thenReturn(testPlaceEntity);
        when(placeMapper.toDTO(testPlaceEntity)).thenReturn(testPlaceDTO);
        
        PlaceDTO updateDTO = new PlaceDTO();
        updateDTO.setName("Updated Place");
        // location is null
        
        PlaceDTO result = placeService.updatePlace("place-1", updateDTO);
        
        assertNotNull(result);
        verify(placeRepository, times(1)).save(any(PlaceEntity.class));
    }
    
    @Test
    void testUpdatePlaceWithLocation() {
        when(placeRepository.findById("place-1")).thenReturn(Optional.of(testPlaceEntity));
        when(placeRepository.save(any(PlaceEntity.class))).thenReturn(testPlaceEntity);
        when(placeMapper.toDTO(testPlaceEntity)).thenReturn(testPlaceDTO);
        
        PlaceDTO updateDTO = new PlaceDTO();
        updateDTO.setName("Updated Place");
        
        LocationDTO newLocation = new LocationDTO();
        newLocation.setCity("New City");
        newLocation.setCountry("New Country");
        newLocation.setLatitude(48.8566);
        newLocation.setLongitude(2.3522);
        updateDTO.setLocation(newLocation);
        
        PlaceDTO result = placeService.updatePlace("place-1", updateDTO);
        
        assertNotNull(result);
        verify(placeRepository, times(1)).save(any(PlaceEntity.class));
    }
}
