package ar.edu.um.ingenieria.limitador.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserData;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserById() {
        var user = new User();
        user.setUsername("jdoe");
        user.setEmail("jdoe@example.com");
        user.setPassword("password123");
        user.setActivated(true);

        var saved = userRepository.save(user);

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("jdoe");
        assertThat(found.get().getEmail()).isEqualTo("jdoe@example.com");
        assertThat(found.get().getActivated()).isTrue();
    }

    @Test
    void shouldFindAllUsers() {
        var user1 = new User();
        user1.setUsername("jdoe");
        user1.setEmail("jdoe@example.com");
        user1.setPassword("password123");
        user1.setActivated(true);

        var user2 = new User();
        user2.setUsername("jane");
        user2.setEmail("jane@example.com");
        user2.setPassword("password456");
        user2.setActivated(false);

        userRepository.save(user1);
        userRepository.save(user2);

        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(2);
    }

    @Test
    void shouldDeleteUser() {
        var user = new User();
        user.setUsername("toDelete");
        user.setEmail("delete@example.com");
        user.setPassword("password123");
        user.setActivated(true);

        var saved = userRepository.save(user);
        Long id = saved.getId();

        userRepository.deleteById(id);

        assertThat(userRepository.findById(id)).isEmpty();
    }

    @Test
    void shouldSaveUserWithRoles() {
        var role = new Role();
        role.setDescription("Admin");
        role.setRoleName("ROLE_ADMIN");

        var user = new User();
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setPassword("password123");
        user.setActivated(true);
        user.getRoles().add(role);

        var saved = userRepository.save(user);

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRoles()).hasSize(1);
    }

    @Test
    void shouldSaveUserWithUserData() {
        var userData = new UserData();
        userData.setFirstName("Juan");
        userData.setPhoneNumber("123456789");

        var user = new User();
        user.setUsername("juan");
        user.setEmail("juan@example.com");
        user.setPassword("password123");
        user.setActivated(true);
        user.setUserData(userData);

        var saved = userRepository.save(user);

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUserData()).isNotNull();
        assertThat(found.get().getUserData().getFirstName()).isEqualTo("Juan");
    }
}
