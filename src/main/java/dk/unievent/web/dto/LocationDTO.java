package dk.unievent.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {
    @NotBlank(message = "Street is required")
    @Size(min = 1, max = 255, message = "Street must be between 1 and 255 characters")
    private String street;
    
    @NotBlank(message = "City is required")
    @Size(min = 1, max = 100, message = "City must be between 1 and 100 characters")
    private String city;
    
    @NotBlank(message = "Zip code is required")
    @Pattern(regexp = "^[0-9]{4,10}$", message = "Zip code must be between 4 and 10 digits")
    private String zip;
    
    @NotBlank(message = "Country is required")
    @Size(min = 1, max = 100, message = "Country must be between 1 and 100 characters")
    private String country;
    
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;
    
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;
}
