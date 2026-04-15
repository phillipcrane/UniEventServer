package dk.unievent.app.tools.services;

import dk.unievent.app.infrastructure.seeding.DataSeederService;
import dk.unievent.app.infrastructure.seeding.SeedResult;
import dk.unievent.app.tools.models.SeedResponse;
import org.springframework.stereotype.Service;

@Service
public class SeedService {

    private final DataSeederService dataSeederService;

    public SeedService(DataSeederService dataSeederService) {
        this.dataSeederService = dataSeederService;
    }

    public SeedResponse seedData() {
        SeedResult result = dataSeederService.seedData();
        return new SeedResponse(result.isSuccess(), result.getMessage(), result.getPageCount(), result.getEventCount(), result.getPlaceCount());
    }

    public SeedResponse clearSeedData() {
        SeedResult result = dataSeederService.clearSeedData();
        return new SeedResponse(result.isSuccess(), result.getMessage(), result.getPageCount(), result.getEventCount(), result.getPlaceCount());
    }
}
