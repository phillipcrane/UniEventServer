package dk.unievent.app.api.dto;

import java.util.List;

public record AdminIngestResponse(
    String pageId,
    int eventCount,
    List<String> eventTitles
) {}
