package dk.unievent.app.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 12, max = 100) String password,
        @Pattern(regexp = "^(user|organizer)$", message = "Role must be either 'user' or 'organizer'") String role
) {}