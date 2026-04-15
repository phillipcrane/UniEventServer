package dk.unievent.app.api.controller;

import dk.unievent.app.application.dto.UserDTO;
import dk.unievent.app.application.service.JwtService;
import dk.unievent.app.application.service.UserService;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.api.dto.AuthResponse;
import dk.unievent.app.api.dto.LoginRequest;
import dk.unievent.app.api.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserEntity user = userService.register(new UserDTO(request.username(), request.email(), request.password()));
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserDetails userDetails = userService.loadUserByUsername(request.email());
        UserEntity user = userService.findByEmail(request.email());
        String token = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getEmail()));
    }
}
