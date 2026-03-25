package dk.unievent.web.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "page_id")
    private String pageId;
    @Column(name = "title")
    private String title;
    @Column(name = "description")
    private String description;
    @Column(name = "start_time")
    private Instant startTime;
    @Column(name = "end_time")
    private Instant endTime;
    @Column(name = "place")
    private String place; // JSON string or separate entity
    @Column(name = "cover_image_url")
    private String coverImageUrl;
    @Column(name = "event_url")
    private String eventURL;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Column(name = "raw")
    private String raw; // JSON string

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getEventURL() { return eventURL; }
    public void setEventURL(String eventURL) { this.eventURL = eventURL; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getRaw() { return raw; }
    public void setRaw(String raw) { this.raw = raw; }
}