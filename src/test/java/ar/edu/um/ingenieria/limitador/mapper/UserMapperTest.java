package ar.edu.um.ingenieria.limitador.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.dto.UserDTO;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("Should map User entity to UserDTO including flattened UserData and role names")
    void shouldMapUserToUserDTO() {
        // Given
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setDescription("Administrator");
        adminRole.setRoleName("ROLE_ADMIN");

        Role userRole = new Role();
        userRole.setId(2L);
        userRole.setDescription("Standard User");
        userRole.setRoleName("ROLE_USER");

        UserData userData = new UserData();
        userData.setId(10L);
        userData.setFirstName("John");
        userData.setLastName("Doe");
        userData.setAddress("Main St 123");
        userData.setPhoneNumber("555-1234");

        User user = new User();
        user.setId(1L);
        user.setUsername("johndoe");
        user.setEmail("john@example.com");
        user.setPassword("secret123");
        user.setActivated(true);
        user.setRoles(Set.of(adminRole, userRole));
        user.setUserData(userData);

        // When
        UserDTO dto = userMapper.toDto(user);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("johndoe");
        assertThat(dto.getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getPassword()).isEqualTo("secret123");
        assertThat(dto.getActivated()).isTrue();
        assertThat(dto.getRoles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getAddress()).isEqualTo("Main St 123");
        assertThat(dto.getPhoneNumber()).isEqualTo("555-1234");
    }

    @Test
    @DisplayName("Should map UserDTO to User entity including UserData and Roles")
    void shouldMapUserDTOToUser() {
        // Given
        UserDTO dto = UserDTO.builder()
            .id(2L)
            .username("janedoe")
            .email("jane@example.com")
            .password("secure456")
            .activated(true)
            .roles(Set.of("ROLE_USER"))
            .firstName("Jane")
            .lastName("Doe")
            .address("Second St 456")
            .phoneNumber("555-5678")
            .build();

        // When
        User user = userMapper.toEntity(dto);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(2L);
        assertThat(user.getUsername()).isEqualTo("janedoe");
        assertThat(user.getEmail()).isEqualTo("jane@example.com");
        assertThat(user.getPassword()).isEqualTo("secure456");
        assertThat(user.getActivated()).isTrue();
        assertThat(user.getRoles()).hasSize(1);
        assertThat(user.getRoles().iterator().next().getRoleName()).isEqualTo("ROLE_USER");
        assertThat(user.getUserData()).isNotNull();
        assertThat(user.getUserData().getFirstName()).isEqualTo("Jane");
        assertThat(user.getUserData().getLastName()).isEqualTo("Doe");
        assertThat(user.getUserData().getAddress()).isEqualTo("Second St 456");
        assertThat(user.getUserData().getPhoneNumber()).isEqualTo("555-5678");
    }

    @Test
    @DisplayName("Should return null when source User is null")
    void shouldReturnNullWhenUserIsNull() {
        assertThat(userMapper.toDto((User) null)).isNull();
    }

    @Test
    @DisplayName("Should return null when source UserDTO is null")
    void shouldReturnNullWhenUserDTOIsNull() {
        assertThat(userMapper.toEntity((UserDTO) null)).isNull();
    }

    @Test
    @DisplayName("Should map list of Users to list of UserDTOs")
    void shouldMapUserListToDtoList() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("u1");
        user1.setEmail("u1@test.com");
        user1.setPassword("p1");
        user1.setActivated(true);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("u2");
        user2.setEmail("u2@test.com");
        user2.setPassword("p2");
        user2.setActivated(false);

        List<UserDTO> dtoList = userMapper.toDtoList(List.of(user1, user2));

        assertThat(dtoList).hasSize(2);
        assertThat(dtoList.get(0).getUsername()).isEqualTo("u1");
        assertThat(dtoList.get(1).getUsername()).isEqualTo("u2");
    }
}
