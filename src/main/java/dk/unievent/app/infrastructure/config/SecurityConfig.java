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
            CsrfValidationFilter csrfValidationFilter,
            CookieConfig cookieConfig
    ) throws Exception {
        boolean devProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");

        http
            .authorizeHttpRequests(authz -> {
                authz
                    // Frontend SPA - static assets served from resources/static (Vite build output)
                    .requestMatchers(org.springframework.http.HttpMethod.GET,
                            "/", "/index.html", "/assets/**", "/favicon.ico", "/favicon.svg", "/favicon.png").permitAll()
                    .requestMatchers("/api/auth/organizer-key/generate").hasRole("admin")
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("admin")
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/**").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/**").authenticated()
                    .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/**").authenticated()
                    .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/**").authenticated()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/media/**").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/media/**").authenticated()
                    .requestMatchers("/admin/**").hasRole("admin")
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").denyAll();
                if (devProfile) {
                    authz.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();
                } else {
                    authz.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").hasRole("admin");
                }
                authz.anyRequest().authenticated();
            })
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> {})
            // CSRF is handled by CsrfValidationFilter; Spring's built-in filter is disabled.
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
                .addHeaderWriter((request, response) ->
                    response.setHeader("X-CSRF-Token", "required")
                )
                .addHeaderWriter((request, response) ->
                    response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()")
                )
            )
            .addFilterBefore(cookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(csrfValidationFilter, CookieAuthenticationFilter.class);

        return http.build();
    }
}
