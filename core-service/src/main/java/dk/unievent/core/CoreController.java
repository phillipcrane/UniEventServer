package dk.unievent.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/core")
/**
 * Exposes core orchestration metadata and service discovery information.
 */
public class CoreController {

    @Value("${services.facebook.url:http://localhost:8081}")
    private String facebookServiceUrl;

    @Value("${services.secret-manager.url:http://localhost:8082}")
    private String secretManagerServiceUrl;

    @Value("${services.storage.url:http://localhost:8083}")
    private String storageServiceUrl;

    /**
     * Lightweight health endpoint for core-service availability checks.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "core-service",
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Returns the current service-role contract and configured downstream URLs.
     */
    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> capabilities() {
        return ResponseEntity.ok(Map.of(
                "service", "core-service",
                "role", "orchestration-and-discovery",
                "downstreamServices", Map.of(
                        "facebook", facebookServiceUrl,
                        "secretManager", secretManagerServiceUrl,
                        "storage", storageServiceUrl
                )
        ));
    }
}
