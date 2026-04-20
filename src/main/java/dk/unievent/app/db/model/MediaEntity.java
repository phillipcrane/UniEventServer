package dk.unievent.app.db.model;

import jakarta.persistence.*;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "media_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;

    @Column(nullable = false)
    private String contentType;

    /**
     * SeaweedFS file ID (fid) for storage retrieval
     */
    private String fileId;

    @Column(length = 2048)
    private String sourceUrl;

    private Instant uploadedAt;
}
