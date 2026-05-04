package dk.unievent.app.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration
 * 
 * Binds to properties under unievent.security.cors prefix.
 * Defines allowed origins, methods, headers, and credentials policy for /api/** and /media/** endpoints.
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "unievent.security.cors")
public class CorsConfig {

    private final Environment environment;

    public CorsConfig(Environment environment) {
        this.environment = environment;
    }

    private List<String> allowedOrigins = List.of();
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("Content-Type", "Accept", "X-CSRF-Token");
    private boolean allowCredentials = true;
    private Long maxAge = 3600L;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public Long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Long maxAge) {
        this.maxAge = maxAge;
    }

    @PostConstruct
    public void warnOnInsecureDefaults() {
        boolean hasLocalhostOrigin = allowCredentials && allowedOrigins.stream()
                .anyMatch(o -> o.contains("localhost") || o.contains("127.0.0.1"));
        if (!hasLocalhostOrigin) return;

        boolean isProd = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equals("prod") || p.equals("production"));
        if (isProd) {
            log.warn("CORS: allowCredentials=true with localhost origins detected in production - remove localhost from UNIEVENT_SECURITY_CORS_ALLOWED_ORIGINS");
        } else {
            log.debug("CORS: localhost origins configured with allowCredentials=true (dev mode)");
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/media/**", configuration);
        return source;
    }
}
