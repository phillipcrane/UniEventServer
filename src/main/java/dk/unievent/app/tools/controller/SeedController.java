package dk.unievent.app.tools.controller;

import dk.unievent.app.tools.models.SeedResponse;
import dk.unievent.app.tools.services.SeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/tools/seed")
@Tag(name = "Admin Tools - Seeding", description = "Local development - seed and clear test data")
@Profile("dev")
public class SeedController {

    private final SeedService seedService;

    public SeedController(SeedService seedService) {
        this.seedService = seedService;
    }

    @PostMapping
    @Operation(summary = "Seed test data", description = "Insert minimal test data (2 pages, 10 events, 2 places) for local development")
    @ApiResponse(responseCode = "200", description = "Test data seeded successfully")
    @ApiResponse(responseCode = "500", description = "Error during seeding")
    public ResponseEntity<?> seed() {
        log.info("Received seed request");
        SeedResponse result = seedService.seedData();
        return result.isSuccess()
            ? ResponseEntity.ok(result)
            : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    @DeleteMapping
    @Operation(summary = "Clear seeded test data", description = "Remove all test data marked with SEED_ prefix")
    @ApiResponse(responseCode = "200", description = "Test data cleared successfully")
    @ApiResponse(responseCode = "500", description = "Error during cleanup")
    public ResponseEntity<?> clear() {
        log.info("Received clear request");
        SeedResponse result = seedService.clearSeedData();
        return result.isSuccess()
            ? ResponseEntity.ok(result)
            : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}
