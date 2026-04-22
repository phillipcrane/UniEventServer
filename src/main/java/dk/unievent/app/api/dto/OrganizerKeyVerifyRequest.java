package dk.unievent.app.api.dto;

import jakarta.validation.constraints.NotBlank;

public record OrganizerKeyVerifyRequest(
        @NotBlank String key
) {}
