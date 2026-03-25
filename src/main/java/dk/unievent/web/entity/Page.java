package dk.unievent.web.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pages")
public class Page {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name")
    private String name;
    @Column(name = "picture_url")
    private String pictureUrl;
    @Column(name = "instagram_id")
    private String instagramId;
    @Column(name = "token_refreshed_at")
    private Instant tokenRefreshedAt;
    @Column(name = "token_stored_at")
    private Instant tokenStoredAt;
    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;
    @Column(name = "token_expires_in_days")
    private Long tokenExpiresInDays;
    @Column(name = "token_status")
    private String tokenStatus;
    @Column(name = "last_refresh_success")
    private Boolean lastRefreshSuccess;
    @Column(name = "last_refresh_error")
    private String lastRefreshError;
    @Column(name = "last_refresh_attempt")
    private Instant lastRefreshAttempt;
    @Column(name = "active")
    private Boolean active;
    @Column(name = "url")
    private String url;
    @Column(name = "connected_at")
    private Instant connectedAt;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    public String getInstagramId() { return instagramId; }
    public void setInstagramId(String instagramId) { this.instagramId = instagramId; }

    public Instant getTokenRefreshedAt() { return tokenRefreshedAt; }
    public void setTokenRefreshedAt(Instant tokenRefreshedAt) { this.tokenRefreshedAt = tokenRefreshedAt; }

    public Instant getTokenStoredAt() { return tokenStoredAt; }
    public void setTokenStoredAt(Instant tokenStoredAt) { this.tokenStoredAt = tokenStoredAt; }

    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(Instant tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public Long getTokenExpiresInDays() { return tokenExpiresInDays; }
    public void setTokenExpiresInDays(Long tokenExpiresInDays) { this.tokenExpiresInDays = tokenExpiresInDays; }

    public String getTokenStatus() { return tokenStatus; }
    public void setTokenStatus(String tokenStatus) { this.tokenStatus = tokenStatus; }

    public Boolean getLastRefreshSuccess() { return lastRefreshSuccess; }
    public void setLastRefreshSuccess(Boolean lastRefreshSuccess) { this.lastRefreshSuccess = lastRefreshSuccess; }

    public String getLastRefreshError() { return lastRefreshError; }
    public void setLastRefreshError(String lastRefreshError) { this.lastRefreshError = lastRefreshError; }

    public Instant getLastRefreshAttempt() { return lastRefreshAttempt; }
    public void setLastRefreshAttempt(Instant lastRefreshAttempt) { this.lastRefreshAttempt = lastRefreshAttempt; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Instant getConnectedAt() { return connectedAt; }
    public void setConnectedAt(Instant connectedAt) { this.connectedAt = connectedAt; }
}