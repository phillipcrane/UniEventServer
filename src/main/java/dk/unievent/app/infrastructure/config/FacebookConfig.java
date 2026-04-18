package dk.unievent.app.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Facebook integration configuration properties
 * Binds to unievent.facebook.* properties from application.yaml
 */
@Configuration
@ConfigurationProperties(prefix = "unievent.facebook")
@Getter
@Setter
public class FacebookConfig {
    
    private boolean enabled;

    private String graphApiBaseUrl = "https://graph.facebook.com";

    private String appId;
    
    private String appSecret;
    
    private String redirectUri;
    
    private String graphApiVersion;
}
