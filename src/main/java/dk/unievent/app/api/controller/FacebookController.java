package dk.unievent.app.api.controller;

import dk.unievent.app.application.service.FacebookOAuthService;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.infrastructure.config.FacebookConfig;
import dk.unievent.app.infrastructure.constants.FacebookApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/facebook")
@Tag(name = "Facebook Integration", description = "Facebook OAuth and event integration")
public class FacebookController {

    private static final long STATE_MAX_AGE_SECONDS = FacebookApiConstants.STATE_MAX_AGE_SECONDS;

    private final FacebookOAuthService facebookOAuthService;
    private final FacebookConfig facebookConfig;

    @Value("${frontend.url:http://localhost}")
    private String frontendUrl;

    public FacebookController(FacebookOAuthService facebookOAuthService, FacebookConfig facebookConfig) {
        this.facebookOAuthService = facebookOAuthService;
        this.facebookConfig = facebookConfig;
    }

    /**
     * Generates a signed OAuth state and returns the full Facebook authorization URL.
     * The frontend must use this URL to initiate the OAuth flow so that the state
     * can be validated on callback.
     */
    @GetMapping("/auth")
    @Operation(
        summary = "Initiate Facebook OAuth",
        description = "Returns the Facebook authorization URL with a server-signed state parameter for CSRF protection."
    )
    @ApiResponse(responseCode = "200", description = "Authorization URL generated")
    public ResponseEntity<Map<String, String>> initiateOAuth() {
        String state = generateSignedState();
        String authUrl = FacebookApiConstants.OAUTH_URL
            + "?client_id=" + facebookConfig.getAppId()
            + "&redirect_uri=" + URLEncoder.encode(facebookConfig.getRedirectUri(), StandardCharsets.UTF_8)
            + "&scope=" + FacebookApiConstants.OAUTH_SCOPES
            + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8)
            + "&response_type=code";
        return ResponseEntity.ok(Map.of("url", authUrl, "state", state));
    }

    /**
     * OAuth callback endpoint - GET only (POST removed; Facebook only uses redirects).
     * Validates the CSRF state before processing the authorization code.
     */
    @GetMapping("/callback")
    @Operation(
        summary = "Facebook OAuth callback",
        description = "Process OAuth authorization code, exchange for tokens, and store pages."
    )
    @ApiResponse(responseCode = "302", description = "Redirects to frontend with result")
    @ApiResponse(responseCode = "400", description = "Missing or invalid parameters")
    public void handleOAuthCallback(
        @Parameter(description = "Authorization code from Facebook")
        @RequestParam(required = false) String code,
        @Parameter(description = "Signed state parameter for CSRF protection")
        @RequestParam(required = false) String state,
        jakarta.servlet.http.HttpServletResponse response
    ) throws java.io.IOException {
        if (code == null || code.isEmpty()) {
            log.error("OAuth callback missing authorization code");
            response.sendRedirect(frontendUrl + "?error=Missing+authorization+code");
            return;
        }

        try {
            if (!validateState(state)) {
                log.warn("OAuth callback received invalid or expired state parameter");
                response.sendRedirect(frontendUrl + "?error=Invalid+or+expired+state");
                return;
            }

            List<PageEntity> pages = facebookOAuthService.processOAuthCallback(code);
            log.info("OAuth callback processed successfully. {} pages stored", pages.size());
            response.sendRedirect(frontendUrl + "?success=true&pages=" + pages.size());
        } catch (Exception e) {
            log.error("Error processing Facebook OAuth callback", e);
            log.error("Error processing Facebook OAuth callback", e);
            response.sendRedirect(frontendUrl + "?error=oauth_error");
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Facebook integration health check")
    @ApiResponse(responseCode = "200", description = "Facebook service is healthy")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Facebook integration service is running"));
    }

    private String generateSignedState() {
        String appSecret = facebookConfig.getAppSecret();
        if (appSecret == null || appSecret.isEmpty()) {
            throw new IllegalStateException("FACEBOOK_APP_SECRET is not configured - cannot generate signed OAuth state");
        }
        String nonce = UUID.randomUUID().toString().replace("-", "");
        long epoch = java.time.Instant.now().getEpochSecond();
        String data = nonce + "." + epoch;
        String hmac = computeHmac(data, appSecret);
        return data + "." + hmac;
    }

    private boolean validateState(String state) {
        if (state == null) { log.warn("State validation failed: state is null"); return false; }
        int first = state.indexOf('.');
        if (first < 0) { log.warn("State validation failed: missing first dot separator"); return false; }
        int second = state.indexOf('.', first + 1);
        if (second < 0) { log.warn("State validation failed: missing second dot separator"); return false; }

        String nonce = state.substring(0, first);
        String epochStr = state.substring(first + 1, second);
        String providedHmac = state.substring(second + 1);

        try {
            long epoch = Long.parseLong(epochStr);
            long age = java.time.Instant.now().getEpochSecond() - epoch;
            if (age > STATE_MAX_AGE_SECONDS) {
                log.warn("State validation failed: state expired (age={}s, max={}s)", age, STATE_MAX_AGE_SECONDS);
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("State validation failed: epoch is not a number: {}", epochStr);
            return false;
        }

        String appSecret = facebookConfig.getAppSecret();
        if (appSecret == null || appSecret.isEmpty()) {
            log.error("State validation failed: FACEBOOK_APP_SECRET is not configured");
            return false;
        }

        String expectedHmac = computeHmac(nonce + "." + epochStr, appSecret);
        boolean match = MessageDigest.isEqual(
            expectedHmac.getBytes(StandardCharsets.UTF_8),
            providedHmac.getBytes(StandardCharsets.UTF_8)
        );
        if (!match) log.warn("State validation failed: HMAC mismatch");
        return match;
    }

    private String computeHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hmacBytes.length * 2);
            for (byte b : hmacBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }
}
