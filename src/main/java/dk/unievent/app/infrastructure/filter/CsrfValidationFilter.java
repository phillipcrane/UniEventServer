package dk.unievent.app.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.unievent.app.application.service.CsrfTokenService;
import dk.unievent.app.infrastructure.config.CookieConfig;
import dk.unievent.app.infrastructure.exception.CsrfValidationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Order(1)
@RequiredArgsConstructor
public class CsrfValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CsrfValidationFilter.class);
    private static final Set<String> STATE_CHANGING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String CSRF_HEADER = "X-CSRF-Token";

    private final CsrfTokenService csrfTokenService;
    private final CookieConfig cookieConfig;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!requiresValidation(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerToken = request.getHeader(CSRF_HEADER);
        Cookie csrfCookie = WebUtils.getCookie(request, cookieConfig.getCsrfName());
        String cookieToken = csrfCookie == null ? null : csrfCookie.getValue();

        if (!csrfTokenService.validateToken(headerToken, cookieToken)) {
            log.warn("CSRF validation failed for {} {}", request.getMethod(), request.getRequestURI());
            writeForbiddenResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresValidation(HttpServletRequest request) {
        if (isSafeMethod(request.getMethod())) {
            return false;
        }

        if (!STATE_CHANGING_METHODS.contains(request.getMethod())) {
            return false;
        }

        if ("/api/auth/refresh".equals(request.getRequestURI()) || "/api/auth/csrf-token".equals(request.getRequestURI())) {
            return false;
        }

        Cookie accessCookie = WebUtils.getCookie(request, cookieConfig.getAccessName());
        return accessCookie != null;
    }

    private boolean isSafeMethod(String method) {
        return HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method)
                || HttpMethod.TRACE.matches(method);
    }

    private void writeForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        CsrfValidationException exception = new CsrfValidationException();

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("error", "Forbidden");
        body.put("message", exception.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
