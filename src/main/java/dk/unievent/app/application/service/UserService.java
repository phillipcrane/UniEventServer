package dk.unievent.app.application.service;

import dk.unievent.app.application.dto.UserDTO;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dk.unievent.app.infrastructure.constants.RoleConstants;
import dk.unievent.app.infrastructure.security.UserDetailsAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService, ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:cli@unievent.internal}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_PASSWORD not set - CLI admin account not provisioned");
            return;
        }
        userRepository.findByEmail(adminEmail).ifPresentOrElse(existing -> {
            if (!passwordEncoder.matches(adminPassword, existing.getPassword())) {
                log.error("CLI admin account {} exists but ADMIN_PASSWORD does not match - not touching it (possible pre-registration attack?)", adminEmail);
                return;
            }
            if (!RoleConstants.ADMIN.equals(existing.getRole())) {
                existing.setRole(RoleConstants.ADMIN);
                userRepository.save(existing);
                log.warn("CLI admin account role corrected to ADMIN: {}", adminEmail);
            } else {
                log.debug("CLI admin account already exists: {}", adminEmail);
            }
        }, () -> {
            userRepository.save(UserEntity.builder()
                    .username("cli")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(RoleConstants.ADMIN)
                    .build());
            log.info("CLI admin account provisioned: {}", adminEmail);
        });
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new UserDetailsAdapter(user);
    }

    public UserEntity register(UserDTO user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        // Validate role against allowlist - only 'user' and 'organizer' are permitted for self-registration.
        // 'admin' role elevation requires an authorized admin-only flow.
        String requestedRole = user.getRole();
        if (requestedRole == null || requestedRole.isBlank()) {
            requestedRole = RoleConstants.USER;
        }
        String validatedRole = validateRegistrationRole(requestedRole);
        UserEntity account = UserEntity.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .password(passwordEncoder.encode(user.getPassword()))
                .role(validatedRole)
                .build();
        return userRepository.save(account);
    }

    /**
     * Validates and returns a role for self-registration.
     * Only 'user' is allowed for self-registration. 'organizer' role requires an admin-issued invitation key.
     * 'admin' role requires authorized admin-only action.
     * This prevents privilege escalation attacks during registration.
     */
    private String validateRegistrationRole(String role) {
        if (role == null || role.isBlank() || RoleConstants.USER.equalsIgnoreCase(role)) {
            return RoleConstants.USER;
        }
        if (RoleConstants.ORGANIZER.equalsIgnoreCase(role)) {
            log.warn("Organizer role requested during self-registration - not allowed. Use the invitation key flow instead.");
            throw new IllegalArgumentException("Organizer role cannot be self-registered. Contact an admin for an invitation key.");
        }
        log.warn("Invalid role requested during registration: {}. Defaulting to 'user'.", role);
        throw new IllegalArgumentException("Invalid role: " + role + ". Only 'user' is allowed for self-registration.");
    }

    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
