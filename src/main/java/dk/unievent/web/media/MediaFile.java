package dk.unievent.web.media;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "media_files")
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

    // getters and setters

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
