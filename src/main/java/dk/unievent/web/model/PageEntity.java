package dk.unievent.web.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pages")
@Getter
@Setter
@NoArgsConstructor
public class PageEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "pictureId")
    private MediaEntity picture;

    // Token management fields
    private LocalDateTime tokenRefreshedAt;
    private LocalDateTime tokenStoredAt;
    private LocalDateTime tokenExpiresAt;
    private Integer tokenExpiresInDays;
    private String tokenStatus;

    // Refresh tracking
    private Boolean lastRefreshSuccess;
    private String lastRefreshError;
    private LocalDateTime lastRefreshAttempt;

    // Metadata
    @Column(name = "createdAt", insertable = true, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "connectedAt")
    private LocalDateTime connectedAt;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventEntity> events;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (connectedAt == null) {
            connectedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
