package dk.unievent.app.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dk.unievent.app.application.dto.PlaceDTO;
import dk.unievent.app.application.mapper.PlaceMapper;
import dk.unievent.app.db.model.PlaceEntity;
import dk.unievent.app.db.repository.PlaceRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Service
public class PlaceService {
    
    @Autowired
    private PlaceRepository placeRepository;
    
    @Autowired
    private PlaceMapper placeMapper;
    
    /**
     * Get a place by ID
     */
    public Optional<PlaceDTO> getPlaceById(String id) {
        log.debug("Fetching place with id: {}", id);
        Optional<PlaceEntity> entity = placeRepository.findById(id);
        if (entity.isEmpty()) {
            log.debug("Place not found with id: {}", id);
        } else {
            log.debug("Place found: {}", id);
        }
        return entity.map(placeMapper::toDTO);
    }
    
    /**
     * Find places by city
     */
    public Page<PlaceDTO> getPlacesByCity(String city, Pageable pageable) {
        log.debug("Fetching places for city: {}", city);
        Page<PlaceDTO> result = placeRepository.findByCity(city, pageable)
            .map(placeMapper::toDTO);
        log.debug("Found {} places in city: {}", result.getTotalElements(), city);
        return result;
    }
    
    /**
     * Find places by country
     */
    public Page<PlaceDTO> getPlacesByCountry(String country, Pageable pageable) {
        log.debug("Fetching places for country: {}", country);
        Page<PlaceDTO> result = placeRepository.findByCountry(country, pageable)
            .map(placeMapper::toDTO);
        log.debug("Found {} places in country: {}", result.getTotalElements(), country);
        return result;
    }
    
    /**
     * Find places by city and country
     */
    public Page<PlaceDTO> getPlacesByCityAndCountry(String city, String country, Pageable pageable) {
        log.debug("Fetching places for city: {}, country: {}", city, country);
        Page<PlaceDTO> result = placeRepository.findByCityAndCountry(city, country, pageable)
            .map(placeMapper::toDTO);
        log.debug("Found {} places in {}, {}", result.getTotalElements(), city, country);
        return result;
    }
    
    /**
     * Search places by name
     */
    public Page<PlaceDTO> searchByName(String name, Pageable pageable) {
        log.debug("Searching places by name: {}", name);
        Page<PlaceDTO> result = placeRepository.findByNameIgnoreCase(name, pageable)
            .map(placeMapper::toDTO);
        log.debug("Search found {} places matching: {}", result.getTotalElements(), name);
        return result;
    }
    
    /**
     * Create a new place
     */
    public PlaceDTO createPlace(PlaceDTO placeDTO) {
        log.info("Creating new place: {}", placeDTO.getName());
        PlaceEntity entity = placeMapper.toEntity(placeDTO);
        PlaceEntity saved = placeRepository.save(entity);
        log.info("Place created successfully with id: {}", saved.getId());
        return placeMapper.toDTO(saved);
    }
    
    /**
     * Update an existing place
     */
    public Optional<PlaceDTO> updatePlace(String id, PlaceDTO placeDTO) {
        log.info("Updating place with id: {}", id);
        Optional<PlaceEntity> existing = placeRepository.findById(id);
        if (existing.isEmpty()) {
            log.warn("Place not found for update with id: {}", id);
            return Optional.empty();
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
        log.info("Place updated successfully: {}", id);
        return Optional.of(placeMapper.toDTO(updated));
    }
    
    /**
     * Delete a place
     */
    public boolean deletePlace(String id) {
        log.info("Deleting place with id: {}", id);
        if (placeRepository.existsById(id)) {
            placeRepository.deleteById(id);
            log.info("Place deleted successfully: {}", id);
            return true;
        }
        log.warn("Place not found for deletion with id: {}", id);
        return false;
    }

    /**
     * Create or find a place by name, city, and country.
     * Used by Facebook event ingestion to resolve or create venues.
     * 
     * @param name Place venue name
     * @param street Street address (optional)
     * @param city City name
     * @param zip Postal code (optional)
     * @param country Country name
     * @return Existing or newly created PlaceEntity
     */
    public PlaceEntity createOrFindPlace(String name, String street, String city, String zip, String country) {
        log.debug("Searching for place: {} in {}, {}", name, city, country);
        
        // Try to find an existing place by scanning all candidates in the same city and country
        int pageNumber = 0;
        int pageSize = 50;
        Page<PlaceEntity> existing;
        
        do {
            existing = placeRepository.findByCityAndCountry(
                    city,
                    country,
                    org.springframework.data.domain.PageRequest.of(pageNumber, pageSize));
            
            for (PlaceEntity place : existing.getContent()) {
                if (place.getName() != null && place.getName().equalsIgnoreCase(name)) {
                    log.debug("Found existing place: {} (id: {})", name, place.getId());
                    return place;
                }
            }
            
            pageNumber++;
        } while (existing.hasNext());
        
        // Place not found, create new one
        log.debug("Place not found, creating new: {} in {}, {}", name, city, country);
        PlaceEntity newPlace = PlaceEntity.builder()
                .name(name)
                .street(street)
                .city(city)
                .zip(zip)
                .country(country)
                .build();
        
        PlaceEntity saved = placeRepository.save(newPlace);
        log.info("New place created: {} (id: {})", name, saved.getId());
        return saved;
    }
}
