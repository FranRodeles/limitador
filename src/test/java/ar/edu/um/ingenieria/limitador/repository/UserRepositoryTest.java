package ar.edu.um.ingenieria.limitador.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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
    void shouldFindUsersWithPagination() {
        var user1 = new User();
        user1.setUsername("pagedUser1");
        user1.setEmail("paged1@example.com");
        user1.setPassword("password123");
        user1.setActivated(true);

        var user2 = new User();
        user2.setUsername("pagedUser2");
        user2.setEmail("paged2@example.com");
        user2.setPassword("password123");
        user2.setActivated(true);

        var user3 = new User();
        user3.setUsername("pagedUser3");
        user3.setEmail("paged3@example.com");
        user3.setPassword("password123");
        user3.setActivated(true);

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

        Page<User> firstPage = userRepository.findAll(PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(firstPage.getTotalPages()).isGreaterThanOrEqualTo(2);
        assertThat(firstPage.getNumber()).isZero();
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

    @Test
    void shouldFindUserByUsernameAndEmail() {
        var user = new User();
        user.setUsername("searchUser");
        user.setEmail("search@example.com");
        user.setPassword("secret123");
        user.setActivated(true);
        userRepository.save(user);

        Optional<User> found = userRepository.findByUsernameAndEmail("searchUser", "search@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("searchUser");
        assertThat(found.get().getEmail()).isEqualTo("search@example.com");
    }

    @Test
    void shouldReturnEmptyWhenUsernameOrEmailDoesNotMatch() {
        var user = new User();
        user.setUsername("existingUser");
        user.setEmail("existing@example.com");
        user.setPassword("secret123");
        user.setActivated(true);
        userRepository.save(user);

        Optional<User> wrongEmail = userRepository.findByUsernameAndEmail("existingUser", "wrong@example.com");
        Optional<User> wrongUser = userRepository.findByUsernameAndEmail("wrongUser", "existing@example.com");

        assertThat(wrongEmail).isEmpty();
        assertThat(wrongUser).isEmpty();
    }
}
