package dk.unievent.web.repository;

import dk.unievent.web.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void saveUser_shouldPersistUser() {
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john.doe@uni.dk");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("john.doe@uni.dk");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByEmail_shouldReturnUser_whenEmailExists() {
        User user = new User();
        user.setName("Jane Doe");
        user.setEmail("jane.doe@uni.dk");
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("jane.doe@uni.dk");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Jane Doe");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenEmailDoesNotExist() {
        Optional<User> found = userRepository.findByEmail("nonexistent@uni.dk");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@uni.dk");
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("test@uni.dk")).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenEmailDoesNotExist() {
        assertThat(userRepository.existsByEmail("notexist@uni.dk")).isFalse();
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        User user1 = new User();
        user1.setName("User One");
        user1.setEmail("user1@uni.dk");
        userRepository.save(user1);

        User user2 = new User();
        user2.setName("User Two");
        user2.setEmail("user2@uni.dk");
        userRepository.save(user2);

        List<User> all = userRepository.findAll();

        assertThat(all).hasSize(2);
    }

    @Test
    void deleteById_shouldRemoveUser() {
        User user = new User();
        user.setName("To Delete");
        user.setEmail("delete@uni.dk");
        User saved = userRepository.save(user);

        userRepository.deleteById(saved.getId());

        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }
}
