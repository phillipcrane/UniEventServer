package dk.unievent.app.application.dto;

public record TokenRefreshResult(
    String pageId,
    boolean success,
    String message
) {}
