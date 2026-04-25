package dk.unievent.app.api.controller;

import dk.unievent.app.application.dto.UserDTO;
import dk.unievent.app.application.service.RefreshTokenService;
import dk.unievent.app.application.service.UserService;
import dk.unievent.app.application.service.OrganizerKeyService;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.api.dto.AuthResponse;
import dk.unievent.app.api.dto.LogoutRequest;
import dk.unievent.app.api.dto.LoginRequest;
import dk.unievent.app.api.dto.RefreshRequest;
import dk.unievent.app.api.dto.RegisterRequest;
import dk.unievent.app.api.dto.OrganizerKeyVerifyRequest;
import dk.unievent.app.api.dto.OrganizerKeyVerifyResponse;
import dk.unievent.app.api.dto.OrganizerRegisterWithKeyRequest;
import dk.unievent.app.api.dto.GenerateOrganizerKeyRequest;
import dk.unievent.app.api.dto.GenerateOrganizerKeyResponse;
import dk.unievent.app.infrastructure.security.UserDetailsAdapter;
import dk.unievent.app.application.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final OrganizerKeyService organizerKeyService;
    private final EmailService emailService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account with email, username, and password. All self-registered users get the 'user' role. To register as an organizer, use the invitation key flow.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully registered", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserEntity user = userService.register(new UserDTO(request.username(), request.email(), request.password(), "user"));
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.issueTokenPair(user);
        return ResponseEntity.ok(new AuthResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                user.getUsername(),
                user.getEmail(),
                tokenPair.accessTokenExpiresInMs(),
                tokenPair.refreshTokenExpiresInMs()
        ));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate a user with email and password, returns access and refresh tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully logged in", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserEntity user = ((UserDetailsAdapter) auth.getPrincipal()).getUser();
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.issueTokenPair(user);
        return ResponseEntity.ok(new AuthResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                user.getUsername(),
                user.getEmail(),
                tokenPair.accessTokenExpiresInMs(),
                tokenPair.refreshTokenExpiresInMs()
        ));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token for a new access token and refresh token pair")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token successfully refreshed", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                                HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotate(request.refreshToken(), userAgent, ipAddress);
        return ResponseEntity.ok(new AuthResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.username(),
                tokenPair.email(),
                tokenPair.accessTokenExpiresInMs(),
                tokenPair.refreshTokenExpiresInMs()
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revoke the refresh token to invalidate all active sessions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User successfully logged out"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/organizer-key/generate")
    @PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/register-with-key")
    @Operation(summary = "Register organizer with key", description = "Complete organizer registration using a confirmation token from key verification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Organizer successfully registered", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body or password strength"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired confirmation token"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists"),
            @ApiResponse(responseCode = "422", description = "Confirmation token already used")
    })
    public ResponseEntity<AuthResponse> registerOrganizerWithKey(
            @Valid @RequestBody OrganizerRegisterWithKeyRequest request) {
        UserEntity organizer = organizerKeyService.completeOrganizerRegistration(
                request.confirmationToken(),
                request.username(),
                request.password(),
                request.email()
        );
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.issueTokenPair(organizer);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                organizer.getUsername(),
                organizer.getEmail(),
                tokenPair.accessTokenExpiresInMs(),
                tokenPair.refreshTokenExpiresInMs()
        ));
    }
}
