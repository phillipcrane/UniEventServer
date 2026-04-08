package dk.unievent.app.application.mapper;

import org.springframework.stereotype.Component;

import dk.unievent.app.application.dto.PageDTO;
import dk.unievent.app.db.model.PageEntity;

@Component
public class PageMapper {
    
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
        
        return PageEntity.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
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
