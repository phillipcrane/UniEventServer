package dk.unievent.app.infrastructure.filter;

import dk.unievent.app.application.service.JwtService;
import dk.unievent.app.infrastructure.config.CookieConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.List;

@Component
@Order(0)
@RequiredArgsConstructor
public class CookieAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CookieAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CookieConfig cookieConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Cookie accessCookie = WebUtils.getCookie(request, cookieConfig.getAccessName());

        if (accessCookie != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = accessCookie.getValue();
            String username = jwtService.extractUsername(token);

            if (username != null && jwtService.isTokenValid(token, username)) {
                List<SimpleGrantedAuthority> authorities = jwtService.extractAuthorities(token);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                log.debug("Authenticated request from '{}' cookie for user '{}'", cookieConfig.getAccessName(), username);
            }
        }

        filterChain.doFilter(request, response);
    }
}
