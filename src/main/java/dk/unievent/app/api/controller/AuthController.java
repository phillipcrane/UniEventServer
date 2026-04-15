package dk.unievent.app.api.controller;

import dk.unievent.app.application.dto.UserDTO;
import dk.unievent.app.application.service.RefreshTokenService;
import dk.unievent.app.application.service.UserService;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.api.dto.AuthResponse;
import dk.unievent.app.api.dto.LogoutRequest;
import dk.unievent.app.api.dto.LoginRequest;
import dk.unievent.app.api.dto.RefreshRequest;
import dk.unievent.app.api.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserEntity user = userService.register(new UserDTO(request.username(), request.email(), request.password()));
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserEntity user = userService.findByEmail(request.email());
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
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotate(request.refreshToken());
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
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
