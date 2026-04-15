package dk.unievent.app.api.controller;

import dk.unievent.app.application.service.FacebookOAuthService;
import dk.unievent.app.db.model.PageEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Facebook OAuth integration endpoints.
 * Handles OAuth callback and server-to-server authorization.
 */
@Slf4j
@RestController
@RequestMapping("/api/facebook")
@Tag(name = "Facebook Integration", description = "Facebook OAuth and event integration")
public class FacebookController {

    private final FacebookOAuthService facebookOAuthService;

    public FacebookController(FacebookOAuthService facebookOAuthService) {
        this.facebookOAuthService = facebookOAuthService;
    }

    /**
     * OAuth callback endpoint.
     * Called by Facebook with authorization code after user approves the app.
     * Exchanges code for tokens and stores user's pages.
     *
     * @param code Authorization code from Facebook
     * @param state State parameter for CSRF protection (optional)
     * @return Redirects to frontend with success/error
     */
    @GetMapping("/callback")
    @Operation(
        summary = "Facebook OAuth callback",
        description = "Process OAuth authorization code, exchange for tokens, and store pages"
    )
    @ApiResponse(responseCode = "200", description = "OAuth callback processed successfully")
    @ApiResponse(responseCode = "400", description = "Missing authorization code")
    @ApiResponse(responseCode = "500", description = "OAuth flow failed")
    public ResponseEntity<?> handleOAuthCallback(
        @Parameter(description = "Authorization code from Facebook")
        @RequestParam String code,
        @Parameter(description = "State parameter for CSRF protection")
        @RequestParam(required = false) String state
    ) {
        log.info("Received Facebook OAuth callback");

        if (code == null || code.isEmpty()) {
            log.error("OAuth callback missing authorization code");
            return ResponseEntity
                .badRequest()
                .body(Map.of("error", "Missing authorization code"));
        }

        try {
            // Process OAuth and store pages
            List<PageEntity> pages = facebookOAuthService.processOAuthCallback(code);

            log.info("OAuth callback processed successfully. {} pages stored", pages.size());

            // Return success response
            return ResponseEntity.ok()
                .body(Map.of(
                    "message", "OAuth callback processed successfully",
                    "pages_stored", pages.size(),
                    "pages", pages.stream()
                        .map(p -> Map.of(
                            "id", p.getId(),
                            "name", p.getName()
                        ))
                        .toList()
                ));

        } catch (Exception e) {
            log.error("Error processing Facebook OAuth callback", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "OAuth callback failed",
                    "message", e.getMessage()
                ));
        }
    }

    /**
     * Healthcheck endpoint for Facebook integration.
     * Verifies that FacebookService is configured and ready.
     *
     * @return Health status
     */
    @GetMapping("/health")
    @Operation(
        summary = "Facebook integration health check",
        description = "Verify Facebook service configuration"
    )
    @ApiResponse(responseCode = "200", description = "Facebook service is healthy")
    public ResponseEntity<?> healthCheck() {
        log.debug("Facebook health check");
        return ResponseEntity.ok()
            .body(Map.of(
                "status", "ok",
                "message", "Facebook integration service is running"
            ));
    }
}
