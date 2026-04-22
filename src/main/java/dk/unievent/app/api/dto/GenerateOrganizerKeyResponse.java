package dk.unievent.app.api.dto;

public record GenerateOrganizerKeyResponse(
        String message,
        long expiresIn
) {}
