package dk.unievent.app.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Facebook event data from Graph API
 * Contains event details, location, and cover image info
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FbEventResponse {
    
    private String id;
    
    private String name;
    
    private String description;
    
    @JsonProperty("start_time")
    private LocalDateTime startTime;
    
    @JsonProperty("end_time")
    private LocalDateTime endTime;
    
    private FbPlaceData place;
    
    private FbCoverData cover;
    
    @JsonProperty("event_times")
    private String eventTimes;
    
    private String timezone;
    
    @JsonProperty("is_canceled")
    private Boolean isCanceled;
    
    @JsonProperty("is_online")
    private Boolean isOnline;
    
    @JsonProperty("is_page_owned")
    private Boolean isPageOwned;
}
