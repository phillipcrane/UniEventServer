package dk.unievent.web.service;

import dk.unievent.web.entity.User;
import dk.unievent.web.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Alice Smith");
        user.setEmail("alice@uni.dk");
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        verify(userRepository).findAll();
    }

    @Test
    void getUserById_shouldReturnUser_whenExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice Smith");
    }

    @Test
    void getUserById_shouldReturnEmpty_whenNotExists() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.getUserById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void getUserByEmail_shouldReturnUser_whenEmailExists() {
        when(userRepository.findByEmail("alice@uni.dk")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByEmail("alice@uni.dk");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice Smith");
    }

    @Test
    void createUser_shouldSaveAndReturnUser_whenEmailIsUnique() {
        when(userRepository.existsByEmail("alice@uni.dk")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.createUser(user);

        assertThat(result.getEmail()).isEqualTo("alice@uni.dk");
        verify(userRepository).save(user);
    }

    @Test
    void createUser_shouldThrowException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("alice@uni.dk")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User with email already exists: alice@uni.dk");
    }

    @Test
    void updateUser_shouldUpdateAndReturnUser_whenExists() {
        User updatedDetails = new User();
        updatedDetails.setName("Alice Updated");
        updatedDetails.setEmail("alice.updated@uni.dk");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("alice.updated@uni.dk")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.updateUser(1L, updatedDetails);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_shouldAllowKeepingSameEmail() {
        User updatedDetails = new User();
        updatedDetails.setName("Alice Updated");
        updatedDetails.setEmail("alice@uni.dk");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.updateUser(1L, updatedDetails);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void updateUser_shouldThrowException_whenNewEmailAlreadyExists() {
        User updatedDetails = new User();
        updatedDetails.setName("Alice Updated");
        updatedDetails.setEmail("taken@uni.dk");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@uni.dk")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, updatedDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User with email already exists: taken@uni.dk");
    }

    @Test
    void updateUser_shouldThrowException_whenNotExists() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    @Test
    void deleteUser_shouldCallDeleteById() {
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }
}
