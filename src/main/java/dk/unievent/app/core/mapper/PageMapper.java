package dk.unievent.app.core.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.unievent.app.core.dto.PageDTO;
import dk.unievent.app.mysql.model.MediaEntity;
import dk.unievent.app.mysql.model.PageEntity;
import dk.unievent.app.mysql.repository.MediaRepository;

@Component
public class PageMapper {
    
    @Autowired
    private MediaRepository mediaRepository;
    
    public PageDTO toDTO(PageEntity entity) {
        if (entity == null) {
            return null;
        }
        
        PageDTO dto = new PageDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setPictureId(entity.getPicture() != null ? entity.getPicture().getId() : null);
        
        // Computed fields
        dto.setUrl("https://facebook.com/" + entity.getId());
        dto.setActive(isPageActive(entity));
        
        return dto;
    }
    
    public PageEntity toEntity(PageDTO dto) {
        if (dto == null) {
            return null;
        }
        
        PageEntity.PageEntityBuilder builder = PageEntity.builder()
                .id(dto.getId())
                .name(dto.getName());
        
        // Load picture if ID provided
        if (dto.getPictureId() != null) {
            MediaEntity picture = mediaRepository.findById(dto.getPictureId()).orElse(null);
            builder.picture(picture);
        }
        
        return builder.build();
    }
    
    /**
     * A page is active if its token status is "valid" and not expired
     */
    private Boolean isPageActive(PageEntity entity) {
        if (entity == null) {
            return false;
        }
        return "valid".equals(entity.getTokenStatus());
    }
}
