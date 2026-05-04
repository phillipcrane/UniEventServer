package dk.unievent.app.api.dto;

import dk.unievent.app.application.dto.TokenRefreshResult;

public record AdminRefreshResult(
    String pageId,
    boolean success,
    String message
) {
    public static AdminRefreshResult from(TokenRefreshResult result) {
        return new AdminRefreshResult(result.pageId(), result.success(), result.message());
    }
}
