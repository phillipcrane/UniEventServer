package dk.unievent.app.vault;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "unievent.vault")
public class VaultProperties {

    private boolean enabled = false;
    private String uri = "http://localhost:8200";
    private String token = "";

    /**
     * KV v2 path, e.g. "secret/data/unievent"
     */
    private String secretPath = "secret/data/unievent";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getSecretPath() { return secretPath; }
    public void setSecretPath(String secretPath) { this.secretPath = secretPath; }
}
