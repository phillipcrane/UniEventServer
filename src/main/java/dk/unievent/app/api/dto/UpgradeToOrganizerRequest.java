package dk.unievent.app.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpgradeToOrganizerRequest(
        @NotBlank(message = "Confirmation token is required")
        String confirmationToken
) {}
