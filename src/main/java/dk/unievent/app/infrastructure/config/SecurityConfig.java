package dk.unievent.app.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import dk.unievent.app.infrastructure.filter.JwtAuthenticationFilter;

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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/media/**").authenticated()
                .requestMatchers("/admin/tools/**").permitAll()
                .requestMatchers("/admin/**").authenticated()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").authenticated()
                .requestMatchers("/actuator/**").denyAll()
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

}
