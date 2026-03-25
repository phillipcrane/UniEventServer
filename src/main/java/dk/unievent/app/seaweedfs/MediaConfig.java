package dk.unievent.app.seaweedfs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "unievent.media.seaweedfs")
public class MediaConfig {
    /**
     * SeaweedFS Master URL (e.g., "localhost:9333")
     */
    private String masterUrl = "localhost:9333";

    /**
     * SeaweedFS Volume URL (e.g., "http://localhost:8080")
     */
    private String volumeUrl = "http://localhost:8080";

    /**
     * Replication factor for stored files
     */
    private int replicationFactor = 1;

    public String getMasterUrl() {
        return masterUrl;
    }

    public void setMasterUrl(String masterUrl) {
        this.masterUrl = masterUrl;
    }

    public String getVolumeUrl() {
        return volumeUrl;
    }

    public void setVolumeUrl(String volumeUrl) {
        this.volumeUrl = volumeUrl;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }
}
