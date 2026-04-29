package dk.unievent.app.application.service;

import dk.unievent.app.db.model.RefreshTokenEntity;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.db.repository.RefreshTokenRepository;
import dk.unievent.app.infrastructure.config.JwtConfig;
import dk.unievent.app.infrastructure.exception.TokenCompromisedException;
import dk.unievent.app.infrastructure.exception.UnauthorizedTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Duration CONCURRENT_ROTATION_GRACE = Duration.ofSeconds(10);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserService userService;
    private final JwtConfig jwtConfig;

    @Transactional
    public TokenPair issueTokenPair(UserEntity user) {
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String familyId = UUID.randomUUID().toString();
        String refreshTokenId = UUID.randomUUID().toString();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails, refreshTokenId, familyId);

        refreshTokenRepository.save(buildRefreshToken(user, userDetails.getUsername(), refreshTokenId, familyId, refreshToken, null));

        return new TokenPair(accessToken, refreshToken, jwtConfig.getExpirationMs(), jwtConfig.getRefreshExpirationMs(), user.getUsername(), user.getEmail(), user.getRole());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized TokenPair rotate(String refreshToken) {
        return rotate(refreshToken, null, null);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized TokenPair rotate(String refreshToken, String userAgent, String ipAddress) {
        String userEmail = jwtService.extractRefreshUsername(refreshToken);
        String tokenId = jwtService.extractRefreshTokenId(refreshToken);
        String familyId = jwtService.extractRefreshFamilyId(refreshToken);

        if (userEmail == null || tokenId == null || familyId == null) {
            throw new UnauthorizedTokenException("Session expired. Please login again.");
        }

        RefreshTokenEntity stored = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> revokeAndFail(familyId));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedTokenException("Session expired. Please login again.");
        }

        String incomingHash = hashToken(refreshToken);

        if (stored.getRevokedAt() != null) {
            if (isRecentlyReplacedToken(stored)
                    && MessageDigest.isEqual(stored.getTokenHash().getBytes(StandardCharsets.UTF_8),
                            incomingHash.getBytes(StandardCharsets.UTF_8))) {
                return rotateLatestReplacement(stored, userAgent, ipAddress);
            }
            revokeFamily(familyId);
            throw new TokenCompromisedException();
        }

        if (!MessageDigest.isEqual(stored.getTokenHash().getBytes(StandardCharsets.UTF_8),
                        incomingHash.getBytes(StandardCharsets.UTF_8))) {
            revokeFamily(familyId);
            throw new TokenCompromisedException();
        }

        UserEntity user;
        UserDetails userDetails;
        try {
            user = userService.findByEmail(userEmail);
            userDetails = userService.loadUserByUsername(userEmail);
        } catch (UsernameNotFoundException ex) {
            revokeFamily(familyId);
            throw new UnauthorizedTokenException("User account no longer exists.");
        }

        String nextTokenId = UUID.randomUUID().toString();
        String nextRefreshToken = jwtService.generateRefreshToken(userDetails, nextTokenId, familyId);
        String accessToken = jwtService.generateAccessToken(userDetails);

        stored.setRevokedAt(Instant.now());
        stored.setReplacedByTokenId(nextTokenId);
        refreshTokenRepository.save(stored);

        refreshTokenRepository.save(buildRefreshToken(user, userEmail, nextTokenId, familyId, nextRefreshToken, userAgent, ipAddress));

        return new TokenPair(accessToken, nextRefreshToken, jwtConfig.getExpirationMs(), jwtConfig.getRefreshExpirationMs(), user.getUsername(), user.getEmail(), user.getRole());
    }

    private TokenPair rotateLatestReplacement(RefreshTokenEntity replacedToken, String userAgent, String ipAddress) {
        RefreshTokenEntity latest = findLatestReplacement(replacedToken);

        if (latest.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedTokenException("Session expired. Please login again.");
        }

        if (latest.getRevokedAt() != null) {
            revokeFamily(latest.getFamilyId());
            throw new TokenCompromisedException();
        }

        UserEntity user;
        UserDetails userDetails;
        try {
            user = userService.findByEmail(latest.getUserEmail());
            userDetails = userService.loadUserByUsername(latest.getUserEmail());
        } catch (UsernameNotFoundException ex) {
            revokeFamily(latest.getFamilyId());
            throw new UnauthorizedTokenException("User account no longer exists.");
        }

        String nextTokenId = UUID.randomUUID().toString();
        String nextRefreshToken = jwtService.generateRefreshToken(userDetails, nextTokenId, latest.getFamilyId());
        String accessToken = jwtService.generateAccessToken(userDetails);

        latest.setRevokedAt(Instant.now());
        latest.setReplacedByTokenId(nextTokenId);
        refreshTokenRepository.save(latest);

        refreshTokenRepository.save(buildRefreshToken(user, latest.getUserEmail(), nextTokenId, latest.getFamilyId(), nextRefreshToken, userAgent, ipAddress));

        return new TokenPair(accessToken, nextRefreshToken, jwtConfig.getExpirationMs(), jwtConfig.getRefreshExpirationMs(), user.getUsername(), user.getEmail(), user.getRole());
    }

    private RefreshTokenEntity findLatestReplacement(RefreshTokenEntity token) {
        RefreshTokenEntity latest = token;
        while (latest.getRevokedAt() != null && latest.getReplacedByTokenId() != null) {
            String nextTokenId = latest.getReplacedByTokenId();
            latest = refreshTokenRepository.findByTokenId(nextTokenId)
                    .orElseThrow(() -> revokeAndFail(token.getFamilyId()));
        }
        return latest;
    }

    private boolean isRecentlyReplacedToken(RefreshTokenEntity token) {
        return token.getReplacedByTokenId() != null
                && token.getRevokedAt() != null
                && token.getRevokedAt().plus(CONCURRENT_ROTATION_GRACE).isAfter(Instant.now());
    }

    @Transactional
    public void logout(String refreshToken) {
        String familyId = jwtService.extractRefreshFamilyId(refreshToken);
        String tokenId = jwtService.extractRefreshTokenId(refreshToken);

        if (familyId == null || tokenId == null) {
            throw new UnauthorizedTokenException("Invalid refresh token.");
        }

        RefreshTokenEntity stored = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new UnauthorizedTokenException("Refresh token not recognized."));

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);
        revokeFamily(familyId);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        List<RefreshTokenEntity> expiredTokens = refreshTokenRepository.findAllByExpiresAtBefore(Instant.now());
        refreshTokenRepository.deleteAll(expiredTokens);
    }

    private RefreshTokenEntity buildRefreshToken(UserEntity user, String userEmail, String tokenId, String familyId, String rawToken, String userAgent) {
        return buildRefreshToken(user, userEmail, tokenId, familyId, rawToken, userAgent, null);
    }

    private RefreshTokenEntity buildRefreshToken(UserEntity user, String userEmail, String tokenId, String familyId, String rawToken, String userAgent, String ipAddress) {
        return RefreshTokenEntity.builder()
                .tokenId(tokenId)
                .familyId(familyId)
                .tokenHash(hashToken(rawToken))
                .userId(user.getId())
                .userEmail(userEmail)
                .expiresAt(Instant.now().plusMillis(jwtConfig.getRefreshExpirationMs()))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
    }

    @Transactional
    private void revokeFamily(String familyId) {
        refreshTokenRepository.revokeByFamilyId(familyId, Instant.now());
    }

    private TokenCompromisedException revokeAndFail(String familyId) {
        revokeFamily(familyId);
        return new TokenCompromisedException();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInMs,
            long refreshTokenExpiresInMs,
            String username,
            String email,
            String role
    ) {}
}
