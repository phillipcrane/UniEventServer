package dk.unievent.app.application.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaDTO {

    private Long id;
    private String filename;
    private String contentType;
    private String fileId;
    private Instant uploadedAt;
}