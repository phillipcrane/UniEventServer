package dk.unievent.app.db.repository;

import dk.unievent.app.db.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
            .username("repotest-user")
            .email("repotest@example.com")
            .password("encodedpassword")
            .build();
        userRepository.save(testUser);
    }

    @Test
    void saveShouldSetTimestamps() {
        UserEntity user = UserEntity.builder()
            .username("timestamps-user")
            .email("timestamps@example.com")
            .password("encoded")
            .build();

        UserEntity saved = userRepository.save(user);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void findByEmailShouldReturnUserWhenExists() {
        Optional<UserEntity> found = userRepository.findByEmail("repotest@example.com");

        assertTrue(found.isPresent());
        assertEquals("repotest-user", found.get().getUsername());
    }

    @Test
    void findByEmailShouldReturnEmptyWhenNotFound() {
        Optional<UserEntity> found = userRepository.findByEmail("nobody@example.com");

        assertTrue(found.isEmpty());
    }

    @Test
    void existsByEmailShouldReturnTrueWhenExists() {
        assertTrue(userRepository.existsByEmail("repotest@example.com"));
    }

    @Test
    void existsByEmailShouldReturnFalseWhenNotFound() {
        assertFalse(userRepository.existsByEmail("nobody@example.com"));
    }

    @Test
    void existsByUsernameShouldReturnTrueWhenExists() {
        assertTrue(userRepository.existsByUsername("repotest-user"));
    }

    @Test
    void existsByUsernameShouldReturnFalseWhenNotFound() {
        assertFalse(userRepository.existsByUsername("nobody"));
    }

    @Test
    void saveShouldThrowOnDuplicateEmail() {
        UserEntity duplicate = UserEntity.builder()
            .username("other-user")
            .email("repotest@example.com")
            .password("encoded")
            .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    void saveShouldThrowOnDuplicateUsername() {
        UserEntity duplicate = UserEntity.builder()
            .username("repotest-user")
            .email("other@example.com")
            .password("encoded")
            .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    void defaultRoleShouldBeUser() {
        UserEntity user = userRepository.findByEmail("repotest@example.com").orElseThrow();
        assertEquals("user", user.getRole());
    }
}
