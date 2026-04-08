package dk.unievent.app.application.mapper;

import org.springframework.stereotype.Component;

import dk.unievent.app.application.dto.EventDTO;
import dk.unievent.app.db.model.EventEntity;

@Component
public class EventMapper {

    private final PlaceMapper placeMapper;

    public EventMapper(PlaceMapper placeMapper) {
        this.placeMapper = placeMapper;
    }

    public EventDTO toDTO(EventEntity entity) {
        if (entity == null) {
            return null;
        }

        EventDTO dto = new EventDTO();
        dto.setId(entity.getId());
        dto.setPageId(entity.getPage() != null ? entity.getPage().getId() : null);
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setPlace(placeMapper.toDTO(entity.getPlace()));
        dto.setCoverImageId(entity.getCoverImage() != null ? entity.getCoverImage().getId() : null);
        dto.setEventUrl(entity.getEventUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    public EventEntity toEntity(EventDTO dto) {
        if (dto == null) {
            return null;
        }

        return EventEntity.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .place(placeMapper.toEntity(dto.getPlace()))
                .eventUrl(dto.getEventUrl())
                .build();
    }
}
