package dk.unievent.app.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dk.unievent.app.application.dto.PlaceDTO;
import dk.unievent.app.application.service.PlaceService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@RestController
@RequestMapping("/api/places")
@Tag(name = "Places", description = "Manage and search venues/locations for events")
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
    @Operation(summary = "Get place by ID", description = "Retrieve a single venue by its ID")
    @ApiResponse(responseCode = "200", description = "Place found")
    @ApiResponse(responseCode = "404", description = "Place not found")
    public ResponseEntity<PlaceDTO> getPlaceById(@PathVariable @Parameter(description = "Place ID") String id) {
        log.debug("Fetching place with id: {}", id);
        PlaceDTO place = placeService.getPlaceById(id);
        
        if (place == null) {
            log.warn("Place not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        log.debug("Place found: {}", id);
        return ResponseEntity.ok(place);
    }
    
    /**
     * GET /api/places/city/{city}
     * Find ALL venues in a specific CITY
     * 
     * Parameter: city = city name
     * Example: GET /api/places/city/Copenhagen
     * Returns: Page of PlaceDTO for all venues in Copenhagen
     * 
     * Useful for: Map view - show all venues in a city
     */
    @GetMapping("/city/{city}")
    @Operation(summary = "Get places by city", description = "Find all venues in a specific city")
    @ApiResponse(responseCode = "200", description = "Page of places in the city")
    public ResponseEntity<Page<PlaceDTO>> getPlacesByCity(@PathVariable @Parameter(description = "City name") String city, Pageable pageable) {
        log.debug("Fetching places for city: {}, page: {}, size: {}", city, pageable.getPageNumber(), pageable.getPageSize());
        Page<PlaceDTO> places = placeService.getPlacesByCity(city, pageable);
        log.debug("Found {} places in city: {}", places.getTotalElements(), city);
        return ResponseEntity.ok(places);
    }
    
    /**
     * GET /api/places/country/{country}
     * Find ALL venues in a specific COUNTRY
     * 
     * Parameter: country = country name
     * Example: GET /api/places/country/Denmark
     * Returns: Page of PlaceDTO for all venues in Denmark
     * 
     * Use case: Country-level event discovery
     */
    @GetMapping("/country/{country}")
    @Operation(summary = "Get places by country", description = "Find all venues in a specific country")
    @ApiResponse(responseCode = "200", description = "Page of places in the country")
    public ResponseEntity<Page<PlaceDTO>> getPlacesByCountry(@PathVariable @Parameter(description = "Country name") String country, Pageable pageable) {
        log.debug("Fetching places for country: {}, page: {}, size: {}", country, pageable.getPageNumber(), pageable.getPageSize());
        Page<PlaceDTO> places = placeService.getPlacesByCountry(country, pageable);
        log.debug("Found {} places in country: {}", places.getTotalElements(), country);
        return ResponseEntity.ok(places);
    }
    
    /**
     * GET /api/places/location/{city}/{country}
     * Find venues in a SPECIFIC CITY AND COUNTRY combo
     * 
     * Parameters: city, country
     * Example: GET /api/places/location/Copenhagen/Denmark
     * Returns: Page of PlaceDTO for venues in that city/country combo
     * 
     * More precise than just city (useful if city names repeat across countries)
     */
    @GetMapping("/location/{city}/{country}")
    @Operation(summary = "Get places by location", description = "Find venues in a specific city and country combination")
    @ApiResponse(responseCode = "200", description = "Page of places in the location")
    public ResponseEntity<Page<PlaceDTO>> getPlacesByLocation(
            @PathVariable @Parameter(description = "City name") String city,
            @PathVariable @Parameter(description = "Country name") String country,
            Pageable pageable) {
        log.debug("Fetching places for city: {}, country: {}, page: {}, size: {}", city, country, pageable.getPageNumber(), pageable.getPageSize());
        Page<PlaceDTO> places = placeService.getPlacesByCityAndCountry(city, country, pageable);
        log.debug("Found {} places in {}, {}", places.getTotalElements(), city, country);
        return ResponseEntity.ok(places);
    }
    
    /**
     * GET /api/places/search
     * SEARCH venues by name (case-insensitive)
     * 
     * Parameter: name (query param) = partial name to search
     * Example: GET /api/places/search?name=s-huset
     * Returns: Page of PlaceDTO matching that name
     * 
     * Useful for: Autocomplete when user selects a venue for an event
     */
    @GetMapping("/search")
    @Operation(summary = "Search places by name", description = "Search for venues using a partial name match (case-insensitive)")
    @ApiResponse(responseCode = "200", description = "Page of matching places")
    public ResponseEntity<Page<PlaceDTO>> searchPlaces(
            @RequestParam(name = "name") @Parameter(description = "Partial place name to search for") String name,
            Pageable pageable) {
        log.debug("Searching places by name: {}", name);
        Page<PlaceDTO> places = placeService.searchByName(name, pageable);
        log.debug("Search found {} places matching: {}", places.getTotalElements(), name);
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
    @Operation(summary = "Create a new place", description = "Create a new venue/location")
    @ApiResponse(responseCode = "201", description = "Place created successfully")
    public ResponseEntity<PlaceDTO> createPlace(@Valid @RequestBody PlaceDTO placeDTO) {
        log.info("Creating new place: {}", placeDTO.getName());
        PlaceDTO created = placeService.createPlace(placeDTO);
        log.info("Place created successfully with id: {}", created.getId());
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
    @Operation(summary = "Update a place", description = "Update venue information")
    @ApiResponse(responseCode = "200", description = "Place updated successfully")
    @ApiResponse(responseCode = "404", description = "Place not found")
    public ResponseEntity<PlaceDTO> updatePlace(
            @PathVariable @Parameter(description = "Place ID") String id,
            @Valid @RequestBody PlaceDTO placeDTO) {
        log.info("Updating place with id: {}", id);
        placeDTO.setId(id);  // Ensure we're updating the right place
        PlaceDTO updated = placeService.updatePlace(id, placeDTO);
        
        if (updated == null) {
            log.warn("Place not found for update with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Place updated successfully: {}", id);
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
    @Operation(summary = "Delete a place", description = "Delete a venue/location")
    @ApiResponse(responseCode = "204", description = "Place deleted successfully")
    @ApiResponse(responseCode = "404", description = "Place not found")
    public ResponseEntity<Void> deletePlace(@PathVariable @Parameter(description = "Place ID") String id) {
        log.info("Deleting place with id: {}", id);
        boolean deleted = placeService.deletePlace(id);
        
        if (!deleted) {
            log.warn("Place not found for deletion with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Place deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }
}
