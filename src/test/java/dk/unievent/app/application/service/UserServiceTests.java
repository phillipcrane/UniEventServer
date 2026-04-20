package dk.unievent.app.application.service;

import dk.unievent.app.application.dto.UserDTO;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.db.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
            .username("testuser")
            .email("test@example.com")
            .password("encodedpassword")
            .role("user")
            .build();
    }

    @Test
    void registerShouldSaveUserWithEncodedPassword() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("rawpassword")).thenReturn("encodedpassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        UserEntity result = userService.register(new UserDTO("testuser", "test@example.com", "rawpassword", "user"));

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(passwordEncoder).encode("rawpassword");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void registerShouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> userService.register(new UserDTO("testuser", "test@example.com", "password", "user")));

        assertEquals("Email is already registered.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldThrowWhenUsernameAlreadyTaken() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> userService.register(new UserDTO("testuser", "test@example.com", "password", "user")));

        assertEquals("Username is already taken.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByEmailShouldReturnUserWhenFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        UserEntity result = userService.findByEmail("test@example.com");

        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void findByEmailShouldThrowWhenNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
            () -> userService.findByEmail("missing@example.com"));
    }

    @Test
    void loadUserByUsernameShouldReturnUserDetailsWhenFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        UserDetails details = userService.loadUserByUsername("test@example.com");

        assertEquals("test@example.com", details.getUsername());
        assertEquals("encodedpassword", details.getPassword());
    }

    @Test
    void loadUserByUsernameShouldThrowWhenNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
            () -> userService.loadUserByUsername("missing@example.com"));
    }

    @Test
    void registerShouldSaveCustomRole() {
        UserEntity organizerUser = UserEntity.builder()
            .username("organizer")
            .email("organizer@example.com")
            .password("encodedpassword")
            .role("organizer")
            .build();

        when(userRepository.existsByEmail("organizer@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("organizer")).thenReturn(false);
        when(passwordEncoder.encode("rawpassword")).thenReturn("encodedpassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(organizerUser);

        UserEntity result = userService.register(new UserDTO("organizer", "organizer@example.com", "rawpassword", "organizer"));

        assertNotNull(result);
        assertEquals("organizer", result.getRole());
    }

    @Test
    void registerShouldDefaultToUserRoleWhenNull() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("rawpassword")).thenReturn("encodedpassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        UserEntity result = userService.register(new UserDTO("testuser", "test@example.com", "rawpassword", null));

        assertNotNull(result);
        assertEquals("user", result.getRole());
    }
}
