package dk.unievent.app.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.*;

/**
 * Data Transfer Object for Pages - what the frontend receives
 * Only exposes public-facing information, hides all internal token/refresh tracking
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO {
    private String id;
    
    @NotBlank(message = "Page name is required")
    @Size(min = 1, max = 255, message = "Page name must be between 1 and 255 characters")
    private String name;
    
    @Pattern(regexp = "^https://facebook\\.com/.*", message = "URL must be a valid Facebook page URL")
    private String url;
    
    private Boolean active;
    
    private Long pictureId;
}
