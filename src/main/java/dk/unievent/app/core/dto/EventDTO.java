package dk.unievent.app.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Events - what the frontend receives
 * Matches the Event interface from web/src/types.ts
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {
    private String id;
    
    @NotBlank(message = "Page ID is required")
    private String pageId;
    
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
    
    @Valid
    private PlaceDTO place;
    
    private Long coverImageId;
    
    @Pattern(regexp = "^https?://[\\w\\-]+(\\.[\\w\\-]+)+.*", message = "Event URL must be a valid URL")
    private String eventURL;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
