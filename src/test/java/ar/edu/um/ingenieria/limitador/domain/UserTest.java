package ar.edu.um.ingenieria.limitador.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;


class UserTest {

    @Test
    void shouldCreateUserWithMultipleRoles() {
        var adminRole = new Role();
        adminRole.setDescription("ADMIN");
        adminRole.setRoleName("ROLE_ADMIN");

        var managerRole = new Role();
        managerRole.setDescription("MANAGER");
        managerRole.setRoleName("ROLE_MANAGER");

        var user = new User();
        user.setUsername("jdoe");
        user.setEmail("jdoe@example.com");
        user.setActivated(true);
        user.setRoles(Set.of(adminRole, managerRole));

        assertThat(user.getUsername()).isEqualTo("jdoe");
        assertThat(user.getEmail()).isEqualTo("jdoe@example.com");
        assertThat(user.getActivated()).isTrue();
        assertThat(user.getRoles()).hasSize(2);
        assertThat(user.getRoles()).containsExactlyInAnyOrder(adminRole, managerRole);
    }

    @Test
    void shouldCreateUserWithSingleRole() {
        var role = new Role();
        role.setDescription("USER");
        role.setRoleName("ROLE_USER");

        var user = new User();
        user.setUsername("jane");
        user.setEmail("jane@example.com");
        user.setActivated(true);
        user.setRoles(Set.of(role));

        assertThat(user.getRoles()).hasSize(1);
        assertThat(user.getRoles()).contains(role);
    }

    @Test
    void shouldCreateUserWithPassword() {
        var user = new User();
        user.setPassword("secretPassword");

        assertThat(user.getPassword()).isEqualTo("secretPassword");
    }
}
