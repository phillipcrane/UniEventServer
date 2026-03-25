package dk.unievent.app.core.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.unievent.app.core.dto.EventDTO;
import dk.unievent.app.mysql.model.EventEntity;
import dk.unievent.app.mysql.model.MediaEntity;
import dk.unievent.app.mysql.model.PageEntity;
import dk.unievent.app.mysql.repository.MediaRepository;
import dk.unievent.app.mysql.repository.PageRepository;

@Component
public class EventMapper {
    
    @Autowired
    private PlaceMapper placeMapper;
    
    @Autowired
    private PageRepository pageRepository;
    
    @Autowired
    private MediaRepository mediaRepository;
    
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
        dto.setEventURL(entity.getEventURL());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        return dto;
    }
    
    public EventEntity toEntity(EventDTO dto) {
        if (dto == null) {
            return null;
        }
        
        EventEntity.EventEntityBuilder builder = EventEntity.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .place(placeMapper.toEntity(dto.getPlace()))
                .eventURL(dto.getEventURL());
        
        // Load cover image if ID provided
        if (dto.getCoverImageId() != null) {
            MediaEntity coverImage = mediaRepository.findById(dto.getCoverImageId()).orElse(null);
            builder.coverImage(coverImage);
        }
        
        // Look up and set the page by pageId
        if (dto.getPageId() != null) {
            PageEntity page = pageRepository.findById(dto.getPageId()).orElse(null);
            builder.page(page);
        }
        
        return builder.build();
    }
}
