package ar.edu.um.ingenieria.limitador.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserDataRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@DataJpaTest
class UserIntegrityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserDataRepository userDataRepository;

    @Test
    @DisplayName("Integrity: should fail when password is null")
    void shouldFailWhenPasswordIsNull() {
        User user = new User();
        user.setUsername("nopassword");
        user.setEmail("nopassword@example.com");
        user.setActivated(true);
        user.setPassword(null);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Integrity: should fail when username is null")
    void shouldFailWhenUsernameIsNull() {
        User user = new User();
        user.setUsername(null);
        user.setEmail("valid@example.com");
        user.setPassword("secret");
        user.setActivated(true);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Integrity: should fail when email is null")
    void shouldFailWhenEmailIsNull() {
        User user = new User();
        user.setUsername("validuser");
        user.setEmail(null);
        user.setPassword("secret");
        user.setActivated(true);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Integrity: should fail when duplicate username is persisted")
    void shouldFailWhenUsernameIsDuplicate() {
        User user1 = new User();
        user1.setUsername("uniqueUser");
        user1.setEmail("first@example.com");
        user1.setPassword("pass1");
        user1.setActivated(true);
        userRepository.saveAndFlush(user1);

        User user2 = new User();
        user2.setUsername("uniqueUser");
        user2.setEmail("second@example.com");
        user2.setPassword("pass2");
        user2.setActivated(true);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Integrity: should fail when duplicate email is persisted")
    void shouldFailWhenEmailIsDuplicate() {
        User user1 = new User();
        user1.setUsername("userA");
        user1.setEmail("same@example.com");
        user1.setPassword("pass1");
        user1.setActivated(true);
        userRepository.saveAndFlush(user1);

        User user2 = new User();
        user2.setUsername("userB");
        user2.setEmail("same@example.com");
        user2.setPassword("pass2");
        user2.setActivated(true);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Integrity: should successfully persist valid user with relations")
    void shouldPersistValidUserWithRelations() {
        Role role = new Role();
        role.setDescription("User Role");
        role.setRoleName("ROLE_USER_TEST");
        Role savedRole = roleRepository.saveAndFlush(role);

        UserData userData = new UserData();
        userData.setFirstName("Jane");
        userData.setLastName("Doe");
        userData.setAddress("Av. San Martin 123");
        userData.setPhoneNumber("261-000000");
        UserData savedUserData = userDataRepository.saveAndFlush(userData);

        User user = new User();
        user.setUsername("validUser");
        user.setEmail("valid@example.com");
        user.setPassword("validPass");
        user.setActivated(true);
        user.setRoles(Set.of(savedRole));
        user.setUserData(savedUserData);
        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getRoles()).hasSize(1);
        assertThat(savedUser.getUserData().getFirstName()).isEqualTo("Jane");
    }
}
