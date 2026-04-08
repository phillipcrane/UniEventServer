package dk.unievent.app.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Bean;

/**
 * OpenAPI/Swagger Configuration
 * 
 * Binds to properties under unievent.openapi prefix.
 * Configures API documentation metadata including title, version, description, contact, and license.
 */
@Component
@ConfigurationProperties(prefix = "unievent.openapi")
public class OpenApiConfig {
    
    private String title = "UniEvent API";
    private String version = "1.0.0";
    private String description = "Event management API for discovering events at Danish venues and organizers";
    private String contactName = "UniEvent Support";
    private String contactEmail = "support@unievent.dk";
    private String licenseName = "MIT";
    private String licenseUrl = "https://opensource.org/licenses/MIT";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(title)
                .version(version)
                .description(description)
                .contact(new Contact()
                    .name(contactName)
                    .email(contactEmail))
                .license(new License()
                    .name(licenseName)
                    .url(licenseUrl)));
    }
}
