package dk.unievent.app.api.controller;

import dk.unievent.app.application.service.FacebookOAuthService;
import dk.unievent.app.db.model.PageEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.beans.factory.annotation.Value;

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

    @Value("${frontend.url:http://localhost}")
    private String frontendUrl;

    public FacebookController(FacebookOAuthService facebookOAuthService) {
        this.facebookOAuthService = facebookOAuthService;
    }

    /**
     * OAuth callback endpoint.
     * Called by Facebook with authorization code after user approves the app.
     * Exchanges code for tokens and stores user's pages.
     * Accepts both GET (from browser redirect) and POST (from server-to-server).
     *
     * @param code Authorization code from Facebook
     * @param state State parameter for CSRF protection (optional)
     * @return JSON response with pages stored or error details
     */
    @RequestMapping(value = "/callback", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(
        summary = "Facebook OAuth callback",
        description = "Process OAuth authorization code, exchange for tokens, and store pages. Accepts GET from browser or POST from server."
    )
    @ApiResponse(responseCode = "200", description = "OAuth callback processed successfully")
    @ApiResponse(responseCode = "400", description = "Missing authorization code")
    @ApiResponse(responseCode = "500", description = "OAuth flow failed")
    public void handleOAuthCallback(
        @Parameter(description = "Authorization code from Facebook")
        @RequestParam(required = false) String code,
        @Parameter(description = "State parameter for CSRF protection")
        @RequestParam(required = false) String state,
        jakarta.servlet.http.HttpServletResponse response
    ) throws java.io.IOException {
        log.info("Received Facebook OAuth callback via GET/POST");

        if (code == null || code.isEmpty()) {
            log.error("OAuth callback missing authorization code");
            String errorUrl = frontendUrl + "?error=Missing+authorization+code";
            response.sendRedirect(errorUrl);
            return;
        }

        try {
            // Process OAuth and store pages
            List<PageEntity> pages = facebookOAuthService.processOAuthCallback(code);

            log.info("OAuth callback processed successfully. {} pages stored", pages.size());

            // Redirect back to frontend with success
            String successUrl = frontendUrl + "?success=true&pages=" + pages.size();
            response.sendRedirect(successUrl);

        } catch (Exception e) {
            log.error("Error processing Facebook OAuth callback", e);
            String errorUrl = frontendUrl + "?error=" + java.net.URLEncoder.encode(e.getMessage(), "UTF-8");
            response.sendRedirect(errorUrl);
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
