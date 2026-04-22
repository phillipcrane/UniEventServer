package dk.unievent.app.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GenerateOrganizerKeyRequest(
        @NotBlank @Email String email,
        String organizationName
) {}
