package dk.unievent.web.controller;

import dk.unievent.web.dto.PlaceDTO;
import dk.unievent.web.service.PlaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for Place endpoints (Venues/Locations)
 * All endpoints are prefixed with /api/places
 * 
 * Places are the VENUES where events happen (bars, cafes, restaurants, etc.)
 * 
 * Example requests:
 * GET  /api/places/{id}                    - Get place by ID
 * GET  /api/places/city/{city}             - Find venues in a city
 * GET  /api/places/country/{country}       - Find venues in a country
 * GET  /api/places/search?name=s-huset     - Search venues by name
 */
@RestController
@RequestMapping("/api/places")
public class PlaceController {
    
    @Autowired
    private PlaceService placeService;
    
    /**
     * GET /api/places/{id}
     * Get a SINGLE place (venue) by ID
     * 
     * Parameter: id = place ID
     * Example: GET /api/places/s-huset-lyngby
     * Returns: PlaceDTO with location details (street, city, country, lat/long)
     * 
     * Example response:
     * {
     *   "id": "s-huset-lyngby",
     *   "name": "S-huset",
     *   "location": {
     *     "street": "Skjoldungsvej 100",
     *     "city": "Lyngby",
     *     "country": "Denmark",
     *     "latitude": 55.7842,
     *     "longitude": 12.4933
     *   }
     * }
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlaceDTO> getPlaceById(@PathVariable String id) {
        PlaceDTO place = placeService.getPlaceById(id);
        
        if (place == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(place);
    }
    
    /**
     * GET /api/places/city/{city}
     * Find ALL venues in a specific CITY
     * 
     * Parameter: city = city name
     * Example: GET /api/places/city/Copenhagen
     * Returns: List of PlaceDTO for all venues in Copenhagen
     * 
     * Useful for: Map view - show all venues in a city
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<List<PlaceDTO>> getPlacesByCity(@PathVariable String city) {
        List<PlaceDTO> places = placeService.getPlacesByCity(city);
        return ResponseEntity.ok(places);
    }
    
    /**
     * GET /api/places/country/{country}
     * Find ALL venues in a specific COUNTRY
     * 
     * Parameter: country = country name
     * Example: GET /api/places/country/Denmark
     * Returns: List of PlaceDTO for all venues in Denmark
     * 
     * Use case: Country-level event discovery
     */
    @GetMapping("/country/{country}")
    public ResponseEntity<List<PlaceDTO>> getPlacesByCountry(@PathVariable String country) {
        List<PlaceDTO> places = placeService.getPlacesByCountry(country);
        return ResponseEntity.ok(places);
    }
    
    /**
     * GET /api/places/location/{city}/{country}
     * Find venues in a SPECIFIC CITY AND COUNTRY combo
     * 
     * Parameters: city, country
     * Example: GET /api/places/location/Copenhagen/Denmark
     * Returns: List of PlaceDTO for venues in that city/country combo
     * 
     * More precise than just city (useful if city names repeat across countries)
     */
    @GetMapping("/location/{city}/{country}")
    public ResponseEntity<List<PlaceDTO>> getPlacesByLocation(
            @PathVariable String city,
            @PathVariable String country) {
        List<PlaceDTO> places = placeService.getPlacesByCityAndCountry(city, country);
        return ResponseEntity.ok(places);
    }
    
    /**
     * GET /api/places/search
     * SEARCH venues by name (case-insensitive)
     * 
     * Parameter: name (query param) = partial name to search
     * Example: GET /api/places/search?name=s-huset
     * Returns: List of PlaceDTO matching that name
     * 
     * Useful for: Autocomplete when user selects a venue for an event
     */
    @GetMapping("/search")
    public ResponseEntity<List<PlaceDTO>> searchPlaces(
            @RequestParam(name = "name") String name) {
        List<PlaceDTO> places = placeService.searchByName(name);
        return ResponseEntity.ok(places);
    }
    
    /**
     * POST /api/places
     * CREATE a new place (venue)
     * 
     * This is called by the event ingestion service when a new venue appears
     * in Facebook event data
     * 
     * Request body: PlaceDTO with venue details
     * Example:
     * {
     *   "id": "pumpehuset",
     *   "name": "Pumpehuset",
     *   "location": {
     *     "street": "Åboulevarden 2",
     *     "city": "Copenhagen",
     *     "country": "Denmark",
     *     "latitude": 55.6751,
     *     "longitude": 12.5747
     *   }
     * }
     * 
     * Returns: Created PlaceDTO (HTTP 201 Created)
     */
    @PostMapping
    public ResponseEntity<PlaceDTO> createPlace(@Valid @RequestBody PlaceDTO placeDTO) {
        PlaceDTO created = placeService.createPlace(placeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * PUT /api/places/{id}
     * UPDATE place information
     * 
     * Parameter: id = place ID to update
     * Request body: PlaceDTO with new values
     * Example: PUT /api/places/s-huset-lyngby
     * Body: { "name": "New Name", "location": { "street": "...", ...} }
     * 
     * Returns: Updated PlaceDTO
     * Returns: HTTP 200 OK, or 404 if not found
     * 
     * Use case: Fix venue address, update name, add/update coordinates
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlaceDTO> updatePlace(
            @PathVariable String id,
            @Valid @RequestBody PlaceDTO placeDTO) {
        placeDTO.setId(id);  // Ensure we're updating the right place
        PlaceDTO updated = placeService.updatePlace(id, placeDTO);
        
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(updated);
    }
    
    /**
     * DELETE /api/places/{id}
     * DELETE a place (venue)
     * 
     * Parameter: id = place ID to delete
     * Example: DELETE /api/places/s-huset-lyngby
     * 
     * Important: This only deletes the place
     * Events at this place are NOT deleted (place field becomes NULL)
     * This is different from deleting a page (which cascades)
     * 
     * Returns: HTTP 204 No Content (success)
     *          HTTP 404 Not Found (place doesn't exist)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlace(@PathVariable String id) {
        boolean deleted = placeService.deletePlace(id);
        
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }
}
