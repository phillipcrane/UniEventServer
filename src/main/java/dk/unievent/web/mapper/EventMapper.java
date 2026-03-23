package dk.unievent.web.mapper;

import dk.unievent.web.dto.EventDTO;
import dk.unievent.web.model.EventEntity;
import dk.unievent.web.model.PageEntity;
import dk.unievent.web.model.MediaEntity;
import dk.unievent.web.repository.MediaRepository;
import dk.unievent.web.repository.PageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        
        EventEntity entity = new EventEntity();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setPlace(placeMapper.toEntity(dto.getPlace()));
        entity.setEventURL(dto.getEventURL());
        
        // Load cover image if ID provided
        if (dto.getCoverImageId() != null) {
            MediaEntity coverImage = mediaRepository.findById(dto.getCoverImageId()).orElse(null);
            entity.setCoverImage(coverImage);
        }
        
        // Look up and set the page by pageId
        if (dto.getPageId() != null) {
            PageEntity page = pageRepository.findById(dto.getPageId()).orElse(null);
            entity.setPage(page);
        }
        
        return entity;
    }
}
