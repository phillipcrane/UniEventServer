package dk.unievent.app.api.dto;

public record OrganizerKeyVerifyResponse(
        String confirmationToken,
        long expiresIn,
        String email
) {}
