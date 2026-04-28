package dk.unievent.app.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.unievent.app.application.service.CsrfTokenService;
import dk.unievent.app.infrastructure.config.CookieConfig;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsrfValidationFilterTests {

    @Mock
    private CsrfTokenService csrfTokenService;

    @Mock
    private CookieConfig cookieConfig;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CsrfValidationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
        lenient().when(cookieConfig.getCsrfName()).thenReturn("csrf_token");
        filter = new CsrfValidationFilter(csrfTokenService, cookieConfig, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRequestsShouldBypassCsrfValidation() throws Exception {
        request.setMethod("GET");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(csrfTokenService);
    }

    @Test
    void postRequestsShouldPassWithValidCsrfToken() throws Exception {
        request.setMethod("POST");
        request.setCookies(new jakarta.servlet.http.Cookie("csrf_token", "cookie-token"));
        request.addHeader("X-CSRF-Token", "header-token");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of())
        );
        when(csrfTokenService.validateToken("header-token", "cookie-token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void postRequestsWithoutHeaderShouldReturnForbidden() throws Exception {
        request.setMethod("POST");
        request.setCookies(new jakarta.servlet.http.Cookie("csrf_token", "cookie-token"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of())
        );
        when(csrfTokenService.validateToken(isNull(), eq("cookie-token"))).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertEquals(403, response.getStatus());
    }

    @Test
    void postRequestsWithMismatchedTokensShouldReturnForbidden() throws Exception {
        request.setMethod("POST");
        request.setCookies(new jakarta.servlet.http.Cookie("csrf_token", "cookie-token"));
        request.addHeader("X-CSRF-Token", "header-token");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of())
        );
        when(csrfTokenService.validateToken("header-token", "cookie-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertEquals(403, response.getStatus());
    }

    @Test
    void deleteRequestsShouldValidateCsrfToken() throws Exception {
        request.setMethod("DELETE");
        request.setCookies(new jakarta.servlet.http.Cookie("csrf_token", "cookie-token"));
        request.addHeader("X-CSRF-Token", "header-token");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of())
        );
        when(csrfTokenService.validateToken("header-token", "cookie-token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void unauthenticatedRequestsShouldBypassCsrfValidation() throws Exception {
        request.setMethod("POST");
        request.setCookies(new jakarta.servlet.http.Cookie("csrf_token", "cookie-token"));
        request.addHeader("X-CSRF-Token", "header-token");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(csrfTokenService);
    }
}
