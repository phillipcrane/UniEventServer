package dk.unievent.app.application.service;

import dk.unievent.app.infrastructure.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

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

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .claim("type", "access")
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
                .claim("family", familyId)
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
        try {
            Claims claims = extractAllClaims(token);
            return userDetails.getUsername().equals(claims.getSubject())
                    && claims.getExpiration().after(new Date())
                    && "access".equals(claims.get("type"));
        } catch (Exception ex) {
            return false;
        }
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
                .verifyWith(refreshToken ? getRefreshSigningKey() : getSigningKey())
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