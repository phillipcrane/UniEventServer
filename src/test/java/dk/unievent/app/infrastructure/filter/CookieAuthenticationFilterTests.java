package dk.unievent.app.infrastructure.filter;

import dk.unievent.app.application.service.JwtService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CookieAuthenticationFilterTests {

    @Mock
    private JwtService jwtService;

    @Mock
    private CookieConfig cookieConfig;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CookieAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
        lenient().when(cookieConfig.getAccessName()).thenReturn("auth_access");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueChainWhenNoAccessCookie() throws Exception {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldPopulateSecurityContextWhenTokenIsValid() throws Exception {
        request.setCookies(new jakarta.servlet.http.Cookie("auth_access", "valid-token"));
        when(jwtService.extractUsername("valid-token")).thenReturn("test@example.com");
        when(jwtService.isTokenValid("valid-token", "test@example.com")).thenReturn(true);
        when(jwtService.extractAuthorities("valid-token"))
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("test@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void shouldNotSetAuthenticationWhenTokenIsInvalid() throws Exception {
        request.setCookies(new jakarta.servlet.http.Cookie("auth_access", "bad-token"));
        when(jwtService.isAccessTokenExpired("bad-token")).thenReturn(false);
        when(jwtService.extractUsername("bad-token")).thenReturn("test@example.com");
        when(jwtService.isTokenValid("bad-token", "test@example.com")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessTokenIsExpired() throws Exception {
        request.setRequestURI("/api/events");
        request.setCookies(new jakarta.servlet.http.Cookie("auth_access", "expired-token"));
        when(jwtService.isAccessTokenExpired("expired-token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Access token expired."));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldAllowRefreshEndpointWhenAccessTokenIsExpired() throws Exception {
        request.setRequestURI("/api/auth/refresh");
        request.setCookies(new jakarta.servlet.http.Cookie("auth_access", "expired-token"));
        when(jwtService.isAccessTokenExpired("expired-token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldContinueChainWhenUsernameNotExtracted() throws Exception {
        request.setCookies(new jakarta.servlet.http.Cookie("auth_access", "orphan-token"));
        when(jwtService.extractUsername("orphan-token")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSkipAuthWhenContextAlreadyHasAuthentication() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing", null, List.of())
        );

        request.setCookies(new jakarta.servlet.http.Cookie("auth_access", "some-token"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(jwtService);
    }
}
