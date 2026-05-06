package dk.unievent.app.application.service;

import dk.unievent.app.infrastructure.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

// issues and validates JWTs. Access tokens and refresh tokens use separate signing keys so a
// compromised access token can't be used to forge a refresh token. Refresh tokens also carry a
// family ID so the server can detect reuse and invalidate the whole family on rotation.
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;

    public String generateToken(UserDetails userDetails) {
        return generateAccessToken(userDetails);
    }

    public String generateAccessToken(UserDetails userDetails) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + jwtConfig.getExpirationMs());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .claim("type", "access")
                .claim("roles", roles)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails, String tokenId, String familyId) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + jwtConfig.getRefreshExpirationMs());

        return Jwts.builder()
                .id(tokenId)
                .subject(userDetails.getUsername())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .claim("type", "refresh")
                .claim("family", familyId) // groups tokens from the same refresh chain so the whole family can be revoked on reuse
                .signWith(getRefreshSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception ex) {
            return null;
        }
    }

    public String extractRefreshUsername(String token) {
        try {
            return extractAllClaims(token, true).getSubject();
        } catch (Exception ex) {
            return null;
        }
    }

    public String extractTokenId(String token) {
        try {
            return extractAllClaims(token).getId();
        } catch (Exception ex) {
            return null;
        }
    }

    public String extractRefreshTokenId(String token) {
        try {
            return extractAllClaims(token, true).getId();
        } catch (Exception ex) {
            return null;
        }
    }

    public String extractRefreshFamilyId(String token) {
        try {
            Object family = extractAllClaims(token, true).get("family");
            return family == null ? null : family.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails.getUsername());
    }

    public boolean isTokenValid(String token, String expectedSubject) {
        try {
            Claims claims = extractAllClaims(token);
            return expectedSubject.equals(claims.getSubject())
                    && claims.getExpiration().after(new Date())
                    && "access".equals(claims.get("type")); // type check rejects refresh tokens used as access tokens
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isAccessTokenExpired(String token) {
        try {
            extractAllClaims(token);
            return false;
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public List<SimpleGrantedAuthority> extractAuthorities(String token) {
        try {
            Object roles = extractAllClaims(token).get("roles");
            if (roles instanceof List<?> list) {
                return list.stream()
                        .map(r -> new SimpleGrantedAuthority(r.toString()))
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token, true);
            return userDetails.getUsername().equals(claims.getSubject())
                    && claims.getExpiration().after(new Date())
                    && "refresh".equals(claims.get("type"));
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return extractAllClaims(token, false);
    }

    private Claims extractAllClaims(String token, boolean refreshToken) {
        return Jwts.parser()
                .verifyWith(refreshToken ? getRefreshSigningKey() : getSigningKey()) // each token type has its own key
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getRefreshSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getRefreshSecret().getBytes(StandardCharsets.UTF_8));
    }
}
