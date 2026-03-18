package dk.unievent.web.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dk.unievent.web.media.MediaFile;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class EventEntity {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "pageId", nullable = false)
    private PageEntity page;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @ManyToOne
    @JoinColumn(name = "placeId")
    private PlaceEntity place;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "coverImageId")
    private MediaFile coverImage;
    
    private String eventURL;

    @Column(name = "createdAt", insertable = true, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
