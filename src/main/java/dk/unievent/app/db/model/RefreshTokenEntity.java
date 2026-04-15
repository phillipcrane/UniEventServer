package dk.unievent.app.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenId;

    @Column(nullable = false, length = 64)
    private String familyId;

    @Column(nullable = false, length = 128)
    private String tokenHash;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String userEmail;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant revokedAt;

    @Column(length = 64)
    private String replacedByTokenId;

    @Column(length = 255)
    private String userAgent;

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}