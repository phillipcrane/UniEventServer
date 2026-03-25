package dk.unievent.app.facebook;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration
 * 
 * By default, Spring Security requires authentication for everything.
 * This config allows PUBLIC access to /api endpoints (no login needed)
 * 
 * In production, you'd add proper authentication here, but for development
 * we'll allow open access to test the API.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Allow public access to /api endpoints (no authentication required)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/**").permitAll()  // All /api/* endpoints are public
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll() // Swagger UI and OpenAPI docs
                .requestMatchers("/actuator/**").permitAll() // Actuator endpoints
                .anyRequest().permitAll()            // Development: allow all other requests
            )
            // Disable CSRF for development (not recommended for production)
            .csrf(csrf -> csrf.disable())
            // Disable default login form
            .httpBasic(basic -> basic.disable());
        
        return http.build();
    }
}
