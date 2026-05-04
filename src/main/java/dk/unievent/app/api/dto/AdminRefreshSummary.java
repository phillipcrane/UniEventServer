package dk.unievent.app.api.dto;

import dk.unievent.app.application.dto.TokenRefreshSummary;

public record AdminRefreshSummary(
    int refreshedCount,
    int failedCount,
    long durationMs
) {
    public static AdminRefreshSummary from(TokenRefreshSummary summary) {
        return new AdminRefreshSummary(summary.refreshedCount(), summary.failedCount(), summary.durationMs());
    }
}
