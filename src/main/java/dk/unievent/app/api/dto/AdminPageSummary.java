package dk.unievent.app.api.dto;

import dk.unievent.app.db.model.PageEntity;

/**
 * Admin-only page summary including token metadata for the CLI page picker.
 */
public record AdminPageSummary(
    String id,
    String name,
    String tokenStatus,
    Integer tokenExpiresInDays
) {
    public static AdminPageSummary from(PageEntity entity) {
        return new AdminPageSummary(
            entity.getId(),
            entity.getName(),
            entity.getTokenStatus(),
            entity.getTokenExpiresInDays()
        );
    }
}
