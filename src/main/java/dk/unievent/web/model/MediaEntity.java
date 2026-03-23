package dk.unievent.web.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "media_files")
@Getter
@Setter
@NoArgsConstructor
public class MediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;

    private String contentType;

    private String path;

    private Instant uploadedAt;

    public MediaEntity(String filename, String contentType, String path) {
        this.filename = filename;
        this.contentType = contentType;
        this.path = path;
        this.uploadedAt = Instant.now();
    }
}
