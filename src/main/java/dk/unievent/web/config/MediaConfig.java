package dk.unievent.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "unievent.media")
public class MediaConfig {
    /**
     * Folder location for storing media files
     */
    private String location = "media";

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
