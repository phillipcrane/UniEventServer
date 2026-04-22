package dk.unievent.app.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizerRegisterWithKeyRequest(
        @NotBlank String confirmationToken,
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 12, max = 100) String password,
        @NotBlank String email
) {}
