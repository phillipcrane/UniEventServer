package dk.unievent.app.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import dk.unievent.app.application.dto.PlaceDTO;
import dk.unievent.app.application.service.PlaceService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/places")
@Tag(name = "Places", description = "Manage and search venues/locations for events")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    
    @GetMapping("/{id}")
    @Operation(summary = "Get place by ID", description = "Retrieve a single venue by its ID")
    @ApiResponse(responseCode = "200", description = "Place found")
    @ApiResponse(responseCode = "404", description = "Place not found")
    public ResponseEntity<PlaceDTO> getPlaceById(@PathVariable @Parameter(description = "Place ID") String id) {
        log.debug("Fetching place with id: {}", id);
        Optional<PlaceDTO> place = placeService.getPlaceById(id);

        if (place.isEmpty()) {
            log.warn("Place not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        log.debug("Place found: {}", id);
        return ResponseEntity.ok(place.get());
    }
    
    @GetMapping("/city/{city}")
    @Operation(summary = "Get places by city", description = "Find all venues in a specific city")
    @ApiResponse(responseCode = "200", description = "Page of places in the city")
    public ResponseEntity<Page<PlaceDTO>> getPlacesByCity(@PathVariable @Parameter(description = "City name") String city, @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching places for city: {}, page: {}, size: {}", city, pageable.getPageNumber(), pageable.getPageSize());
        Page<PlaceDTO> places = placeService.getPlacesByCity(city, pageable);
        log.debug("Found {} places in city: {}", places.getTotalElements(), city);
        return ResponseEntity.ok(places);
    }
    
    @GetMapping("/country/{country}")
    @Operation(summary = "Get places by country", description = "Find all venues in a specific country")
    @ApiResponse(responseCode = "200", description = "Page of places in the country")
    public ResponseEntity<Page<PlaceDTO>> getPlacesByCountry(@PathVariable @Parameter(description = "Country name") String country, @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching places for country: {}, page: {}, size: {}", country, pageable.getPageNumber(), pageable.getPageSize());
        Page<PlaceDTO> places = placeService.getPlacesByCountry(country, pageable);
        log.debug("Found {} places in country: {}", places.getTotalElements(), country);
        return ResponseEntity.ok(places);
    }
    
    @GetMapping("/location/{city}/{country}")
    @Operation(summary = "Get places by location", description = "Find venues in a specific city and country combination")
    @ApiResponse(responseCode = "200", description = "Page of places in the location")
    public ResponseEntity<Page<PlaceDTO>> getPlacesByLocation(
            @PathVariable @Parameter(description = "City name") String city,
            @PathVariable @Parameter(description = "Country name") String country,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching places for city: {}, country: {}, page: {}, size: {}", city, country, pageable.getPageNumber(), pageable.getPageSize());
        Page<PlaceDTO> places = placeService.getPlacesByCityAndCountry(city, country, pageable);
        log.debug("Found {} places in {}, {}", places.getTotalElements(), city, country);
        return ResponseEntity.ok(places);
    }
    
    @GetMapping("/search")
    @RateLimiter(name = "place-search", fallbackMethod = "searchFallback")
    @Operation(summary = "Search places by name", description = "Search for venues using a partial name match (case-insensitive)")
    @ApiResponse(responseCode = "200", description = "Page of matching places")
    public ResponseEntity<Page<PlaceDTO>> searchPlaces(
            @RequestParam(name = "name") @Parameter(description = "Partial place name to search for") String name,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Searching places by name: {}", name);
        Page<PlaceDTO> places = placeService.searchByName(name, pageable);
        log.debug("Search found {} places matching: {}", places.getTotalElements(), name);
        return ResponseEntity.ok(places);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    @RateLimiter(name = "place-create", fallbackMethod = "createFallback")
    @Operation(summary = "Create a new place", description = "Create a new venue/location")
    @ApiResponse(responseCode = "201", description = "Place created successfully")
    public ResponseEntity<PlaceDTO> createPlace(@Valid @RequestBody PlaceDTO placeDTO) {
        log.info("Creating new place: {}", placeDTO.getName());
        PlaceDTO created = placeService.createPlace(placeDTO);
        log.info("Place created successfully with id: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @RateLimiter(name = "place-update", fallbackMethod = "updateFallback")
    @Operation(summary = "Update a place", description = "Update venue information")
    @ApiResponse(responseCode = "200", description = "Place updated successfully")
    @ApiResponse(responseCode = "404", description = "Place not found")
    public ResponseEntity<PlaceDTO> updatePlace(
            @PathVariable @Parameter(description = "Place ID") String id,
            @Valid @RequestBody PlaceDTO placeDTO) {
        log.info("Updating place with id: {}", id);
        placeDTO.setId(id);  // Ensure we're updating the right place
        Optional<PlaceDTO> updated = placeService.updatePlace(id, placeDTO);

        if (updated.isEmpty()) {
            log.warn("Place not found for update with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Place updated successfully: {}", id);
        return ResponseEntity.ok(updated.get());
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @RateLimiter(name = "place-delete", fallbackMethod = "deleteFallback")
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

    public ResponseEntity<Page<PlaceDTO>> searchFallback(String name, Pageable pageable, Exception ex) {
        return ResponseEntity.status(429).build();
    }

    public ResponseEntity<PlaceDTO> createFallback(PlaceDTO placeDTO, Exception ex) {
        return ResponseEntity.status(429).build();
    }

    public ResponseEntity<PlaceDTO> updateFallback(String id, PlaceDTO placeDTO, Exception ex) {
        return ResponseEntity.status(429).build();
    }

    public ResponseEntity<Void> deleteFallback(String id, Exception ex) {
        return ResponseEntity.status(429).build();
    }
}
