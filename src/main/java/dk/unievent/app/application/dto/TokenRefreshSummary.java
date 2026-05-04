package dk.unievent.app.application.dto;

public record TokenRefreshSummary(
    int refreshedCount,
    int failedCount,
    long durationMs
) {}
