package dk.unievent.app.application.service;

import dk.unievent.app.api.dto.OrganizerKeyVerifyResponse;
import dk.unievent.app.db.model.OrganizerKeyEntity;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.db.repository.OrganizerKeyRepository;
import dk.unievent.app.db.repository.UserRepository;
import dk.unievent.app.infrastructure.exception.EmailAlreadyRegisteredException;
import dk.unievent.app.infrastructure.exception.InvalidConfirmationTokenException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyAlreadyUsedException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyExpiredException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyNotFoundException;
import dk.unievent.app.infrastructure.exception.UsernameAlreadyTakenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerKeyService {

    private final OrganizerKeyRepository organizerKeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${unievent.security.jwt.secret}")
    private String jwtSecret;

    @Value("${unievent.organizer-key.expiration-hours:24}")
    private long keyExpirationHours;

    @Value("${unievent.organizer-key.confirmation-token-expiration-minutes:10}")
    private long confirmationTokenExpirationMinutes;

    private static final String CONFIRMATION_TOKEN_TYPE = "organizer-registration-confirmation";
    private static final String KEY_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int KEY_LENGTH = 32;

    public long getKeyExpirationSeconds() {
        return keyExpirationHours * 3600;
    }

    @Transactional
    public String generateOrganizerKey(String email, String adminEmail) {
        UserEntity admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin user not found"));
        String keyValue = generateRandomKey();

        OrganizerKeyEntity keyEntity = OrganizerKeyEntity.builder()
                .keyValue(keyValue)
                .email(email)
                .createdBy(admin.getId())
                .expiresAt(Instant.now().plusSeconds(keyExpirationHours * 3600))
                .usedAt(null)
                .build();

        organizerKeyRepository.save(keyEntity);
        log.info("Generated organizer key for email: {}", email);

        return keyValue;
    }

    /**
     * Verifies a single-use key and returns a confirmation token.
     * Returns a JWT token valid for 10 minutes.
     */
    @Transactional
    public OrganizerKeyVerifyResponse verifyOrganizerKey(String keyValue) {
        OrganizerKeyEntity keyEntity = organizerKeyRepository.findByKeyValue(keyValue)
                .orElseThrow(OrganizerKeyNotFoundException::new);

        if (keyEntity.getUsedAt() != null) {
            throw new OrganizerKeyAlreadyUsedException();
        }

        if (Instant.now().isAfter(keyEntity.getExpiresAt())) {
            throw new OrganizerKeyExpiredException();
        }

        // Generate confirmation token
        String confirmationToken = generateConfirmationToken(keyEntity.getId(), keyEntity.getEmail());

        log.info("Verified organizer key for email: {}", keyEntity.getEmail());

        return new OrganizerKeyVerifyResponse(
                confirmationToken,
                confirmationTokenExpirationMinutes * 60,
                keyEntity.getEmail()
        );
    }

    /**
     * Completes the registration with a confirmation token.
     * Creates a new organizer account.
     */
    @Transactional
    public UserEntity completeOrganizerRegistration(String confirmationToken, String username, String password, String email) {
        // Validate confirmation token
        Long keyId = validateConfirmationToken(confirmationToken);

        // Verify the key still exists and hasn't been used
        OrganizerKeyEntity keyEntity = organizerKeyRepository.findById(keyId)
                .orElseThrow(OrganizerKeyNotFoundException::new);

        if (keyEntity.getUsedAt() != null) {
            throw new OrganizerKeyAlreadyUsedException();
        }

        if (Instant.now().isAfter(keyEntity.getExpiresAt())) {
            throw new OrganizerKeyExpiredException();
        }

        if (!keyEntity.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Email does not match the key");
        }

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyTakenException();
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        // Create new organizer user
        UserEntity organizer = UserEntity.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role("organizer")
                .build();

        userRepository.save(organizer);

        // Mark the key as used
        keyEntity.setUsedAt(Instant.now());
        organizerKeyRepository.save(keyEntity);

        log.info("Completed organizer registration for email: {}", email);

        return organizer;
    }

    /**
     * Generates a random key of 32 characters.
     */
    private String generateRandomKey() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(KEY_LENGTH);

        for (int i = 0; i < KEY_LENGTH; i++) {
            sb.append(KEY_ALPHABET.charAt(random.nextInt(KEY_ALPHABET.length())));
        }

        return sb.toString();
    }

    /**
     * Generates a JWT confirmation token with 10-minute expiration.
     */
    private String generateConfirmationToken(Long keyId, String email) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + confirmationTokenExpirationMinutes * 60 * 1000);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .claim("type", CONFIRMATION_TOKEN_TYPE)
                .claim("keyId", keyId)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validates a confirmation token and returns the key ID.
     */
    private Long validateConfirmationToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Verify token type
            if (!CONFIRMATION_TOKEN_TYPE.equals(claims.get("type"))) {
                throw new IllegalStateException("Invalid token type");
            }

            // Verify not expired
            if (claims.getExpiration().before(new Date())) {
                throw new IllegalStateException("Confirmation token has expired");
            }

            // Extract keyId
            Object keyIdObj = claims.get("keyId");
            if (keyIdObj == null) {
                throw new IllegalStateException("Invalid token: missing keyId");
            }

            return Long.valueOf(keyIdObj.toString());
        } catch (Exception ex) {
            log.warn("Invalid confirmation token: {}", ex.getMessage());
            throw new InvalidConfirmationTokenException();
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
