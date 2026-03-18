package dk.unievent.web.media;

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
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;

    private String contentType;

    private String path;

    private Instant uploadedAt;

    protected MediaFile() {
        // JPA
    }

    public MediaFile(String filename, String contentType, String path) {
        this.filename = filename;
        this.contentType = contentType;
        this.path = path;
        this.uploadedAt = Instant.now();
    }
} 
