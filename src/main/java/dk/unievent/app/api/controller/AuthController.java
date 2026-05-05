package dk.unievent.app.api.controller;

import dk.unievent.app.application.dto.UserDTO;
import dk.unievent.app.application.service.RefreshTokenService;
import dk.unievent.app.application.service.UserService;
import dk.unievent.app.application.service.OrganizerKeyService;
import dk.unievent.app.application.service.CsrfTokenService;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.api.dto.AuthResponse;
import dk.unievent.app.api.dto.LoginRequest;
import dk.unievent.app.api.dto.ProfileResponse;
import dk.unievent.app.api.dto.RegisterRequest;
import dk.unievent.app.api.dto.OrganizerKeyVerifyRequest;
import dk.unievent.app.api.dto.OrganizerKeyVerifyResponse;
import dk.unievent.app.api.dto.OrganizerRegisterWithKeyRequest;
import dk.unievent.app.api.dto.GenerateOrganizerKeyRequest;
import dk.unievent.app.api.dto.GenerateOrganizerKeyResponse;
import dk.unievent.app.api.dto.UpgradeToOrganizerRequest;
import dk.unievent.app.infrastructure.config.CookieConfig;
import dk.unievent.app.infrastructure.security.UserDetailsAdapter;
import dk.unievent.app.application.service.EmailService;
import dk.unievent.app.infrastructure.config.RoleConstants;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final OrganizerKeyService organizerKeyService;
    private final EmailService emailService;
    private final CsrfTokenService csrfTokenService;
    private final CookieConfig cookieConfig;

    @PostMapping("/register")
    @RateLimiter(name = "register", fallbackMethod = "registerFallback")
    @Operation(summary = "Register a new user", description = "Create a new user account with email, username, and password. All self-registered users get the 'user' role. To register as an organizer, use the invitation key flow.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully registered", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        UserEntity user = userService.register(new UserDTO(request.username(), request.email(), request.password(), RoleConstants.USER));
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.issueTokenPair(user);
        String csrfToken = csrfTokenService.generateToken();
        writeAuthCookies(response, tokenPair, csrfToken);
        return ResponseEntity.ok(buildAuthResponse(user, tokenPair, csrfToken));
    }

    @GetMapping("/csrf-token")
    @RateLimiter(name = "csrf-token", fallbackMethod = "csrfTokenFallback")
    @Operation(summary = "Get CSRF token", description = "Issue a CSRF token and cookie for unauthenticated clients before login or registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSRF token issued"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletResponse response) {
        String csrfToken = csrfTokenService.generateToken();
        addCookie(response, cookieConfig.getCsrfName(), csrfToken, cookieConfig.getCsrfMaxAge(), false);
        return ResponseEntity.ok(Map.of("csrfToken", csrfToken));
    }

    @PostMapping("/login")
    @RateLimiter(name = "login", fallbackMethod = "loginFallback")
    @Operation(summary = "Login user", description = "Authenticate a user with email and password, sets auth cookies, and returns account details plus a CSRF token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully logged in", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserEntity user = ((UserDetailsAdapter) auth.getPrincipal()).getUser();
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.issueTokenPair(user);
        String csrfToken = csrfTokenService.generateToken();
        writeAuthCookies(response, tokenPair, csrfToken);
        return ResponseEntity.ok(buildAuthResponse(user, tokenPair, csrfToken));
    }

    @PostMapping("/refresh")
    @RateLimiter(name = "refresh-token", fallbackMethod = "refreshFallback")
    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh cookie for rotated auth cookies and a new CSRF token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token successfully refreshed", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpRequest,
                                                HttpServletResponse response) {
        String refreshToken = resolveRefreshTokenFromCookie(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotate(refreshToken, userAgent, ipAddress);
        String csrfToken = csrfTokenService.generateToken();
        writeAuthCookies(response, tokenPair, csrfToken);
        UserEntity user = userService.findByEmail(tokenPair.email());
        return ResponseEntity.ok(buildAuthResponse(user, tokenPair, csrfToken));
    }

    @PostMapping("/logout")
    @RateLimiter(name = "logout", fallbackMethod = "logoutFallback")
    @Operation(summary = "Logout user", description = "Revoke the refresh token to invalidate all active sessions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User successfully logged out"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest,
                                       HttpServletResponse response) {
        String refreshToken = resolveOptionalRefreshToken(httpRequest);
        if (refreshToken != null) {
            try {
                refreshTokenService.logout(refreshToken);
            } catch (Exception ex) {
                log.debug("Logout refresh token invalid or already revoked", ex);
            }
        }
        clearAuthCookies(response);
        log.info("User logout processed from ip={}, userAgent={}",
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's role and organizer page names")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved", content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ProfileResponse> getProfile(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserEntity user = userService.findByEmail(auth.getName());
        return ResponseEntity.ok(new ProfileResponse(normalizeRole(user.getRole()), List.of()));
    }

    @PostMapping("/organizer-key/generate")
    @PreAuthorize("hasRole('admin')")
    @RateLimiter(name = "generate-organizer-key", fallbackMethod = "generateKeyFallback")
    @Operation(summary = "Generate organizer invitation key", description = "Admin only: Generate a single-use key for organizer registration (sends key via email)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key successfully generated and sent", content = @Content(schema = @Schema(implementation = GenerateOrganizerKeyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - admin role required"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<GenerateOrganizerKeyResponse> generateOrganizerKey(
            @Valid @RequestBody GenerateOrganizerKeyRequest request,
            Authentication authentication) {
        String key = organizerKeyService.generateOrganizerKey(request.email(), authentication.getName());
        emailService.sendOrganizerInvitationEmailAsync(request.email(), key);

        return ResponseEntity.ok(new GenerateOrganizerKeyResponse(
                "Invitation key has been sent to " + request.email(),
                organizerKeyService.getKeyExpirationSeconds()
        ));
    }

    @PostMapping("/organizer-key/verify")
    @RateLimiter(name = "verify-organizer-key", fallbackMethod = "verifyKeyFallback")
    @Operation(summary = "Verify organizer invitation key", description = "Verify a single-use organizer key and receive a confirmation token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key successfully verified", content = @Content(schema = @Schema(implementation = OrganizerKeyVerifyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body or key"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "410", description = "Key already used"),
            @ApiResponse(responseCode = "401", description = "Key expired")
    })
    public ResponseEntity<OrganizerKeyVerifyResponse> verifyOrganizerKey(
            @Valid @RequestBody OrganizerKeyVerifyRequest request) {
        OrganizerKeyVerifyResponse response = organizerKeyService.verifyOrganizerKey(request.key());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/organizer-key/upgrade")
    @RateLimiter(name = "upgrade-organizer", fallbackMethod = "upgradeOrganizerFallback")
    @Operation(summary = "Upgrade existing account to organizer", description = "Upgrade an already-authenticated user account to organizer role using a confirmation token from key verification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account upgraded to organizer", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body or key email mismatch"),
            @ApiResponse(responseCode = "401", description = "Not authenticated or confirmation token invalid/expired"),
            @ApiResponse(responseCode = "410", description = "Invitation key already used")
    })
    public ResponseEntity<AuthResponse> upgradeToOrganizer(
            @Valid @RequestBody UpgradeToOrganizerRequest request,
            Authentication auth,
            HttpServletResponse response) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserEntity user = organizerKeyService.upgradeToOrganizer(request.confirmationToken(), auth.getName());
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.issueTokenPair(user);
        String csrfToken = csrfTokenService.generateToken();
        writeAuthCookies(response, tokenPair, csrfToken);
        return ResponseEntity.ok(buildAuthResponse(user, tokenPair, csrfToken));
    }

    @PostMapping("/register-with-key")
    @RateLimiter(name = "register-with-key", fallbackMethod = "registerWithKeyFallback")
    @Operation(summary = "Register organizer with key", description = "Complete organizer registration using a confirmation token from key verification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Organizer successfully registered", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body or password strength"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired confirmation token"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists"),
            @ApiResponse(responseCode = "422", description = "Confirmation token already used")
    })
    public ResponseEntity<AuthResponse> registerOrganizerWithKey(
            @Valid @RequestBody OrganizerRegisterWithKeyRequest request,
            HttpServletResponse response) {
        UserEntity organizer = organizerKeyService.completeOrganizerRegistration(
                request.confirmationToken(),
                request.username(),
                request.password(),
                request.email()
        );
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.issueTokenPair(organizer);
        String csrfToken = csrfTokenService.generateToken();
        writeAuthCookies(response, tokenPair, csrfToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildAuthResponse(organizer, tokenPair, csrfToken));
    }

    private String resolveRefreshTokenFromCookie(HttpServletRequest request) {
        jakarta.servlet.http.Cookie refreshCookie = WebUtils.getCookie(request, cookieConfig.getRefreshName());
        if (refreshCookie != null && !refreshCookie.getValue().isBlank()) {
            return refreshCookie.getValue();
        }
        throw new IllegalArgumentException("Refresh token cookie is missing.");
    }

    private String resolveOptionalRefreshToken(HttpServletRequest request) {
        jakarta.servlet.http.Cookie refreshCookie = WebUtils.getCookie(request, cookieConfig.getRefreshName());
        if (refreshCookie != null && !refreshCookie.getValue().isBlank()) {
            return refreshCookie.getValue();
        }
        return null;
    }

    private void writeAuthCookies(HttpServletResponse response, RefreshTokenService.TokenPair tokenPair, String csrfToken) {
        addCookie(response, cookieConfig.getAccessName(), tokenPair.accessToken(), cookieConfig.getAccessMaxAge(), true);
        addCookie(response, cookieConfig.getRefreshName(), tokenPair.refreshToken(), cookieConfig.getRefreshMaxAge(), true);
        addCookie(response, cookieConfig.getCsrfName(), csrfToken, cookieConfig.getCsrfMaxAge(), false);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, cookieConfig.getAccessName(), true);
        clearCookie(response, cookieConfig.getRefreshName(), true);
        clearCookie(response, cookieConfig.getCsrfName(), false);
    }

    private void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds, boolean httpOnly) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path(cookieConfig.getPath())
                .httpOnly(httpOnly)
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .maxAge(maxAgeSeconds);

        if (cookieConfig.getDomain() != null && !cookieConfig.getDomain().isBlank()) {
            builder.domain(cookieConfig.getDomain());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void clearCookie(HttpServletResponse response, String name, boolean httpOnly) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .path(cookieConfig.getPath())
                .httpOnly(httpOnly)
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .maxAge(0);

        if (cookieConfig.getDomain() != null && !cookieConfig.getDomain().isBlank()) {
            builder.domain(cookieConfig.getDomain());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private AuthResponse buildAuthResponse(UserEntity user, RefreshTokenService.TokenPair tokenPair, String csrfToken) {
        String normalizedRole = normalizeRole(user.getRole());
        return new AuthResponse(
                user.getUsername(),
                user.getEmail(),
                List.of(normalizedRole),
                csrfToken,
                tokenPair.accessTokenExpiresInMs()
        );
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_USER";
        }
        String trimmed = role.trim();
        if (trimmed.startsWith("ROLE_")) {
            return trimmed;
        }
        return "ROLE_" + trimmed.toUpperCase();
    }

    // Rate limit fallback methods - only intercept RequestNotPermitted (actual rate limit).
    // All other exceptions are re-thrown so the global exception handler maps them correctly.
    private static void rethrowIfNotRateLimited(Exception ex) {
        if (ex instanceof RequestNotPermitted) return;
        if (ex instanceof RuntimeException re) throw re;
        throw new RuntimeException(ex);
    }

    public ResponseEntity<AuthResponse> registerFallback(RegisterRequest request, HttpServletResponse response, Exception ex) {
        rethrowIfNotRateLimited(ex);
        return ResponseEntity.status(429).body(null);
    }

    public ResponseEntity<Map<String, String>> csrfTokenFallback(HttpServletResponse response, Exception ex) {
        rethrowIfNotRateLimited(ex);
        return ResponseEntity.status(429).body(null);
    }

    public ResponseEntity<AuthResponse> loginFallback(LoginRequest request, HttpServletResponse response, Exception ex) {
        rethrowIfNotRateLimited(ex);
        return ResponseEntity.status(429).body(null);
    }

    public ResponseEntity<AuthResponse> refreshFallback(HttpServletRequest httpRequest, HttpServletResponse response, Exception ex) {
        rethrowIfNotRateLimited(ex);
        return ResponseEntity.status(429).body(null);
    }

    public ResponseEntity<Void> logoutFallback(HttpServletRequest httpRequest, HttpServletResponse response, Exception ex) {
        rethrowIfNotRateLimited(ex);
        return ResponseEntity.status(429).build();
    }

    public ResponseEntity<GenerateOrganizerKeyResponse> generateKeyFallback(GenerateOrganizerKeyRequest request, Authentication authentication, Exception ex) {
        rethrowIfNotRateLimited(ex);
        return ResponseEntity.status(429).build();
    }

    public ResponseEntity<OrganizerKeyVerifyResponse> verifyKeyFallback(OrganizerKeyVerifyRequest request, Exception ex) {
        rethrowIfNotRateLimited(ex);
        return ResponseEntity.status(429).build();
    }

    public ResponseEntity<AuthResponse> registerWithKeyFallback(OrganizerRegisterWithKeyRequest request, HttpServletResponse response, Exception ex) {
        rethrowIfNotRateLimited(ex);
        return ResponseEntity.status(429).body(null);
    }

    public ResponseEntity<AuthResponse> upgradeOrganizerFallback(UpgradeToOrganizerRequest request, Authentication auth, HttpServletResponse response, Exception ex) {
        rethrowIfNotRateLimited(ex);
        return ResponseEntity.status(429).body(null);
    }
}
