package dk.unievent.app.application.service;

import dk.unievent.app.application.dto.UserDTO;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            if (!"admin".equals(existing.getRole())) {
                existing.setRole("admin");
                userRepository.save(existing);
                log.warn("CLI admin account role corrected to admin: {}", adminEmail);
            } else {
                log.debug("CLI admin account already exists: {}", adminEmail);
            }
        }, () -> {
            userRepository.save(UserEntity.builder()
                    .username("cli")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role("admin")
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
        String role = user.getRole() != null && !user.getRole().isBlank() ? user.getRole() : "user";
        UserEntity account = UserEntity.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .password(passwordEncoder.encode(user.getPassword()))
                .role(role)
                .build();
        return userRepository.save(account);
    }

    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
