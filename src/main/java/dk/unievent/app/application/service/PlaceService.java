package dk.unievent.app.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.unievent.app.application.dto.PlaceDTO;
import dk.unievent.app.application.mapper.PlaceMapper;
import dk.unievent.app.db.model.PlaceEntity;
import dk.unievent.app.db.repository.PlaceRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlaceService {
    
    @Autowired
    private PlaceRepository placeRepository;
    
    @Autowired
    private PlaceMapper placeMapper;
    
    /**
     * Get a place by ID
     */
    public PlaceDTO getPlaceById(String id) {
        Optional<PlaceEntity> entity = placeRepository.findById(id);
        return entity.map(placeMapper::toDTO).orElse(null);
    }
    
    /**
     * Find places by city
     */
    public List<PlaceDTO> getPlacesByCity(String city) {
        return placeRepository.findByCity(city)
            .stream()
            .map(placeMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Find places by country
     */
    public List<PlaceDTO> getPlacesByCountry(String country) {
        return placeRepository.findByCountry(country)
            .stream()
            .map(placeMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Find places by city and country
     */
    public List<PlaceDTO> getPlacesByCityAndCountry(String city, String country) {
        return placeRepository.findByCityAndCountry(city, country)
            .stream()
            .map(placeMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Search places by name
     */
    public List<PlaceDTO> searchByName(String name) {
        return placeRepository.findByNameIgnoreCase(name)
            .stream()
            .map(placeMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Create a new place
     */
    public PlaceDTO createPlace(PlaceDTO placeDTO) {
        PlaceEntity entity = placeMapper.toEntity(placeDTO);
        PlaceEntity saved = placeRepository.save(entity);
        return placeMapper.toDTO(saved);
    }
    
    /**
     * Update an existing place
     */
    public PlaceDTO updatePlace(String id, PlaceDTO placeDTO) {
        Optional<PlaceEntity> existing = placeRepository.findById(id);
        if (existing.isEmpty()) {
            return null;
        }
        
        PlaceEntity entity = existing.get();
        entity.setName(placeDTO.getName());
        if (placeDTO.getLocation() != null) {
            entity.setStreet(placeDTO.getLocation().getStreet());
            entity.setCity(placeDTO.getLocation().getCity());
            entity.setZip(placeDTO.getLocation().getZip());
            entity.setCountry(placeDTO.getLocation().getCountry());
            entity.setLatitude(placeDTO.getLocation().getLatitude());
            entity.setLongitude(placeDTO.getLocation().getLongitude());
        }
        
        PlaceEntity updated = placeRepository.save(entity);
        return placeMapper.toDTO(updated);
    }
    
    /**
     * Delete a place
     */
    public boolean deletePlace(String id) {
        if (placeRepository.existsById(id)) {
            placeRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
