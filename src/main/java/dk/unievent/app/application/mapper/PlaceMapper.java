package dk.unievent.app.application.mapper;

import org.springframework.stereotype.Component;

import dk.unievent.app.application.dto.LocationDTO;
import dk.unievent.app.application.dto.PlaceDTO;
import dk.unievent.app.db.model.PlaceEntity;

@Component
public class PlaceMapper {
    
    public PlaceDTO toDTO(PlaceEntity entity) {
        if (entity == null) {
            return null;
        }
        
        PlaceDTO dto = new PlaceDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setLocation(toLocationDTO(entity));
        
        return dto;
    }
    
    private LocationDTO toLocationDTO(PlaceEntity entity) {
        LocationDTO location = new LocationDTO();
        location.setStreet(entity.getStreet());
        location.setCity(entity.getCity());
        location.setZip(entity.getZip());
        location.setCountry(entity.getCountry());
        location.setLatitude(entity.getLatitude());
        location.setLongitude(entity.getLongitude());
        
        return location;
    }
    
    public PlaceEntity toEntity(PlaceDTO dto) {
        if (dto == null) {
            return null;
        }
        
        PlaceEntity.PlaceEntityBuilder builder = PlaceEntity.builder()
                .id(dto.getId())
                .name(dto.getName());
        
        if (dto.getLocation() != null) {
            builder.street(dto.getLocation().getStreet())
                    .city(dto.getLocation().getCity())
                    .zip(dto.getLocation().getZip())
                    .country(dto.getLocation().getCountry())
                    .latitude(dto.getLocation().getLatitude())
                    .longitude(dto.getLocation().getLongitude());
        }
        
        return builder.build();
    }
}
