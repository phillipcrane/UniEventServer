package dk.unievent.app.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.*;
import dk.unievent.app.infrastructure.constants.ValidationConstants;
import jakarta.validation.Valid;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDTO {
    private String id;
    
    @NotBlank(message = "Place name is required")
    @Size(min = 1, max = ValidationConstants.NAME_MAX_LENGTH, message = "Place name must be between 1 and 255 characters")
    private String name;
    
    @Valid
    private LocationDTO location;
}
