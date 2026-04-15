package dk.unievent.app.tools.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeedResponse {
    private final boolean success;
    private final String message;
    private final long pageCount;
    private final long eventCount;
    private final long placeCount;
}
