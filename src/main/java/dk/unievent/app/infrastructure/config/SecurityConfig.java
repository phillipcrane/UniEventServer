package dk.unievent.app.infrastructure.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;

import dk.unievent.app.infrastructure.filter.CookieAuthenticationFilter;
import dk.unievent.app.infrastructure.filter.CsrfValidationFilter;
import dk.unievent.app.infrastructure.filter.JwtAuthenticationFilter;

import java.util.Arrays;

/**
 * Security Configuration
 *
 * Frontend SPA:
 *   /, /index.html, /assets/**, /favicon.* - public (static Vite build output)
 *
 * Actuator strategy:
 *   /actuator/health - public (Docker probes and load balancer checks cannot authenticate)
 *   /actuator/info  - public (static build metadata, no secrets)
 *   /actuator/**    - denyAll (metrics and other endpoints blocked in production)
 *
 * Swagger strategy:
 *   dev profile  - public (local development convenience)
 *   all others   - ADMIN only
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ForwardedHeaderFilter());
        bean.setOrder(0);
        return bean;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CookieAuthenticationFilter cookieAuthenticationFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CsrfValidationFilter csrfValidationFilter
    ) throws Exception {
        boolean devProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");

        http
            .authorizeHttpRequests(authz -> {
                authz
                    // Frontend SPA - static assets served from resources/static (Vite build output)
                    .requestMatchers(org.springframework.http.HttpMethod.GET,
                            "/", "/index.html", "/assets/**", "/favicon.ico", "/favicon.svg", "/favicon.png").permitAll()
                    .requestMatchers("/api/auth/organizer-key/generate").hasRole("ADMIN")
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/**").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/**").authenticated()
                    .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/**").authenticated()
                    .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/**").authenticated()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/media/**").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/media/**").authenticated()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").denyAll();
                if (devProfile) {
                    authz.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();
                } else {
                    authz.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").hasRole("ADMIN");
                }
                authz.anyRequest().authenticated();
            })
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> {})
            // Custom CSRF filter is used to validate cookie-authenticated state-changing requests.
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            )
            .addFilterBefore(cookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(csrfValidationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
