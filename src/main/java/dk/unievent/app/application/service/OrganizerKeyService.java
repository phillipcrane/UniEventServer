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
import dk.unievent.app.infrastructure.constants.RoleConstants;
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

// manages single-use organizer invitation keys. flow: admin generates a 32-char key, the recipient
// verifies it (getting a short-lived JWT confirmation token back), then submits the token to
// complete registration or upgrade their account. the key is marked used only after that final step.
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerKeyService {

    private final OrganizerKeyRepository organizerKeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${unievent.security.organizer-key.confirmation-secret}")
    private String confirmationSecret;

    @Value("${unievent.security.organizer-key.expiration-hours:24}")
    private long keyExpirationHours;

    @Value("${unievent.security.organizer-key.confirmation-token-expiration-minutes:10}")
    private long confirmationTokenExpirationMinutes;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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

    // verifies a single-use key and returns a short-lived JWT confirmation token (10 min).
    // the token embeds the keyId so the registration step can look it up without another key lookup.
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

        String confirmationToken = generateConfirmationToken(keyEntity.getId(), keyEntity.getEmail());

        log.info("Verified organizer key for email: {}", keyEntity.getEmail());

        return new OrganizerKeyVerifyResponse(
                confirmationToken,
                confirmationTokenExpirationMinutes * 60,
                keyEntity.getEmail()
        );
    }

    @Transactional
    public UserEntity completeOrganizerRegistration(String confirmationToken, String username, String password, String email) {
        // 1. validate the confirmation token and re-check the key (it could have been used in a race)
        Long keyId = validateConfirmationToken(confirmationToken);
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

        // 2. create the organizer account and mark the key used atomically in the same transaction
        UserEntity organizer = UserEntity.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role("organizer")
                .build();
        userRepository.save(organizer);

        keyEntity.setUsedAt(Instant.now());
        organizerKeyRepository.save(keyEntity);

        log.info("Completed organizer registration for email: {}", email);

        return organizer;
    }

    // same as completeOrganizerRegistration but for existing accounts: upgrades role rather than creating a new user.
    // the key's email must match the authenticated user's email so you can't use someone else's invite.
    @Transactional
    public UserEntity upgradeToOrganizer(String confirmationToken, String authenticatedEmail) {
        Long keyId = validateConfirmationToken(confirmationToken);

        OrganizerKeyEntity keyEntity = organizerKeyRepository.findById(keyId)
                .orElseThrow(OrganizerKeyNotFoundException::new);

        if (keyEntity.getUsedAt() != null) {
            throw new OrganizerKeyAlreadyUsedException();
        }

        if (Instant.now().isAfter(keyEntity.getExpiresAt())) {
            throw new OrganizerKeyExpiredException();
        }

        if (!keyEntity.getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new IllegalArgumentException("The invitation key was not issued for this account.");
        }

        UserEntity user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        user.setRole(RoleConstants.ORGANIZER);
        userRepository.save(user);

        keyEntity.setUsedAt(Instant.now());
        organizerKeyRepository.save(keyEntity);

        log.info("Upgraded user to organizer: {}", authenticatedEmail);
        return user;
    }

    private String generateRandomKey() {
        StringBuilder sb = new StringBuilder(KEY_LENGTH);

        for (int i = 0; i < KEY_LENGTH; i++) {
            sb.append(KEY_ALPHABET.charAt(SECURE_RANDOM.nextInt(KEY_ALPHABET.length())));
        }

        return sb.toString();
    }

    // short-lived JWT (10 min) that carries the keyId as a claim so registration doesn't need the raw key again
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

    private Long validateConfirmationToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!CONFIRMATION_TOKEN_TYPE.equals(claims.get("type"))) {
                throw new IllegalStateException("Invalid token type");
            }
            if (claims.getExpiration().before(new Date())) {
                throw new IllegalStateException("Confirmation token has expired");
            }

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
        return Keys.hmacShaKeyFor(confirmationSecret.getBytes(StandardCharsets.UTF_8));
    }
}
