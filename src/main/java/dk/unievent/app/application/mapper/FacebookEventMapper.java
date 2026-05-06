package dk.unievent.app.application.mapper;

import dk.unievent.app.api.dto.FbEventResponse;
import dk.unievent.app.application.service.PlaceService;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.PlaceEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * Transforms Facebook event schema to app EventEntity schema
 * Handles normalization of event data and location creation
 */
@Slf4j
@Component
public class FacebookEventMapper {
    
    private final PlaceService placeService;
    
    public FacebookEventMapper(PlaceService placeService) {
        this.placeService = placeService;
    }
    
    /**
     * Map FbEventResponse to EventEntity
     * Handles schema transformation and place creation
     */
    public EventEntity mapToEventEntity(String pageId, FbEventResponse fbEvent) {
        log.debug("Mapping Facebook event {} to EventEntity", fbEvent.getId());
        
        EventEntity event = new EventEntity();
        event.setId(fbEvent.getId());
        event.setTitle(fbEvent.getName());
        event.setDescription(fbEvent.getDescription());
        ZoneId zone = resolveZone(fbEvent.getTimezone(), fbEvent.getId());
        if (fbEvent.getStartTime() != null) {
            event.setStartTime(zone != null
                    ? fbEvent.getStartTime().atZoneSameInstant(zone).toLocalDateTime()
                    : fbEvent.getStartTime().toLocalDateTime());
        }
        if (fbEvent.getEndTime() != null) {
            event.setEndTime(zone != null
                    ? fbEvent.getEndTime().atZoneSameInstant(zone).toLocalDateTime()
                    : fbEvent.getEndTime().toLocalDateTime());
        }
        event.setEventUrl("https://facebook.com/events/" + fbEvent.getId());
        
        // Handle place/location
        if (fbEvent.getPlace() != null && fbEvent.getPlace().getName() != null) {
            PlaceEntity place = placeService.createOrFindPlace(
                    fbEvent.getPlace().getName(),
                    fbEvent.getPlace().getStreet(),
                    fbEvent.getPlace().getCity(),
                    fbEvent.getPlace().getZip(),
                    fbEvent.getPlace().getCountry()
            );
            event.setPlace(place);
        }
        
        // Note: coverImage will be set after download via EventService
        // Note: page relationship will be set via EventService
        
        log.debug("Successfully mapped Facebook event {} to EventEntity", fbEvent.getId());
        return event;
    }

    private ZoneId resolveZone(String timezone, String eventId) {
        if (timezone != null) {
            try {
                return ZoneId.of(timezone);
            } catch (Exception e) {
                log.warn("Unrecognised timezone '{}' for event {}, falling back to UTC", timezone, eventId);
                return ZoneId.of("UTC");
            }
        }
        return null;
    }
}
