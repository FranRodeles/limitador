package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.UserDTO;
import ar.edu.um.ingenieria.limitador.mapper.UserMapper;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceDtoTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User createUser(Long id, String username, String email, String password, Boolean activated) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setActivated(activated);
        return user;
    }

    @Test
    @DisplayName("Should return all users as UserDTOs")
    void shouldReturnAllUsersAsDTOs() {
        User u1 = createUser(1L, "user1", "u1@test.com", "pass", true);
        User u2 = createUser(2L, "user2", "u2@test.com", "pass", true);
        UserDTO dto1 = UserDTO.builder().id(1L).username("user1").email("u1@test.com").build();
        UserDTO dto2 = UserDTO.builder().id(2L).username("user2").email("u2@test.com").build();

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        when(userMapper.toDtoList(List.of(u1, u2))).thenReturn(List.of(dto1, dto2));

        List<UserDTO> result = userService.findAllDTOs();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo("user1");
        assertThat(result.get(1).getUsername()).isEqualTo("user2");
        verify(userRepository, times(1)).findAll();
        verify(userMapper, times(1)).toDtoList(any());
    }

    @Test
    @DisplayName("Should return paged users as UserDTOs without passwords")
    void shouldReturnPagedUsersAsDTOs() {
        User u1 = createUser(1L, "user1", "u1@test.com", "pass", true);
        User u2 = createUser(2L, "user2", "u2@test.com", "pass", true);
        UserDTO dto1 = UserDTO.builder().id(1L).username("user1").email("u1@test.com").password("secret").build();
        UserDTO dto2 = UserDTO.builder().id(2L).username("user2").email("u2@test.com").password("secret").build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(u1, u2), pageable, 2);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toDto(u1)).thenReturn(dto1);
        when(userMapper.toDto(u2)).thenReturn(dto2);

        Page<UserDTO> result = userService.findAllDTOs(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("user1");
        assertThat(result.getContent().get(0).getPassword()).isNull();
        assertThat(result.getContent().get(1).getUsername()).isEqualTo("user2");
        assertThat(result.getContent().get(1).getPassword()).isNull();
        verify(userRepository, times(1)).findAll(pageable);
        verify(userMapper, times(1)).toDto(u1);
        verify(userMapper, times(1)).toDto(u2);
    }

    @Test
    @DisplayName("Should find UserDTO by ID when exists")
    void shouldFindUserDTOByIdWhenExists() {
        User user = createUser(1L, "user1", "u1@test.com", "pass", true);
        UserDTO dto = UserDTO.builder().id(1L).username("user1").email("u1@test.com").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        Optional<UserDTO> result = userService.findDTOById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getUsername()).isEqualTo("user1");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return empty Optional when UserDTO not found by ID")
    void shouldReturnEmptyWhenUserDTONotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<UserDTO> result = userService.findDTOById(99L);

        assertThat(result).isEmpty();
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should save UserDTO via MapStruct and repository")
    void shouldSaveUserDTO() {
        UserDTO inputDto = UserDTO.builder()
            .username("newUser")
            .email("new@test.com")
            .password("secret")
            .activated(true)
            .roles(Set.of("ROLE_USER"))
            .build();

        User entityToSave = createUser(null, "newUser", "new@test.com", "secret", true);
        User savedEntity = createUser(10L, "newUser", "new@test.com", "encodedSecret", true);
        UserDTO expectedDto = UserDTO.builder()
            .id(10L)
            .username("newUser")
            .email("new@test.com")
            .activated(true)
            .roles(Set.of("ROLE_USER"))
            .build();

        when(userMapper.toEntity(inputDto)).thenReturn(entityToSave);
        when(passwordEncoder.encode("secret")).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(entityToSave)).thenReturn(savedEntity);
        when(userMapper.toDto(savedEntity)).thenReturn(expectedDto);

        UserDTO result = userService.saveDTO(inputDto);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUsername()).isEqualTo("newUser");
        verify(userMapper, times(1)).toEntity(inputDto);
        verify(passwordEncoder).encode("secret");
        verify(userRepository, times(1)).save(entityToSave);
        verify(userMapper, times(1)).toDto(savedEntity);
    }

    @Test
    @DisplayName("Should update existing user via UserDTO")
    void shouldUpdateExistingUserDTO() {
        UserDTO updateDto = UserDTO.builder()
            .username("updatedUser")
            .email("updated@test.com")
            .password("newPass")
            .activated(false)
            .build();

        User existingUser = createUser(5L, "oldUser", "old@test.com", "oldPass", true);
        User mappedUser = createUser(null, "updatedUser", "updated@test.com", "newPass", false);
        User savedUser = createUser(5L, "updatedUser", "updated@test.com", "encodedNewPass", false);
        UserDTO resultDto = UserDTO.builder().id(5L).username("updatedUser").email("updated@test.com").build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(existingUser));
        when(userMapper.toEntity(updateDto)).thenReturn(mappedUser);
        when(passwordEncoder.encode("newPass")).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(resultDto);

        UserDTO result = userService.updateDTO(5L, updateDto);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getUsername()).isEqualTo("updatedUser");
        assertThat(mappedUser.getId()).isEqualTo(5L);
        verify(passwordEncoder).encode("newPass");
        verify(userRepository, times(1)).save(mappedUser);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existing UserDTO")
    void shouldThrowWhenUpdatingNonExistingUserDTO() {
        UserDTO updateDto = UserDTO.builder().username("ghost").build();
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateDTO(404L, updateDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found with id: 404");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should find UserDTO by username and email without exposing password")
    void shouldFindUserDTOByUsernameAndEmailWhenExists() {
        User user = createUser(1L, "searchUser", "search@test.com", "secretPass", true);
        UserDTO dtoWithPassword = UserDTO.builder()
            .id(1L)
            .username("searchUser")
            .email("search@test.com")
            .password("secretPass")
            .build();

        when(userRepository.findByUsernameAndEmail("searchUser", "search@test.com")).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dtoWithPassword);

        Optional<UserDTO> result = userService.findDTOByUsernameAndEmail("searchUser", "search@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getUsername()).isEqualTo("searchUser");
        assertThat(result.get().getEmail()).isEqualTo("search@test.com");
        assertThat(result.get().getPassword()).isNull();
        verify(userRepository, times(1)).findByUsernameAndEmail("searchUser", "search@test.com");
    }

    @Test
    @DisplayName("Should return empty Optional when UserDTO not found by username and email")
    void shouldReturnEmptyWhenUserDTONotFoundByUsernameAndEmail() {
        when(userRepository.findByUsernameAndEmail("unknown", "unknown@test.com")).thenReturn(Optional.empty());

        Optional<UserDTO> result = userService.findDTOByUsernameAndEmail("unknown", "unknown@test.com");

        assertThat(result).isEmpty();
        verify(userRepository, times(1)).findByUsernameAndEmail("unknown", "unknown@test.com");
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should return empty Optional when search parameters are null or blank")
    void shouldReturnEmptyWhenSearchParametersAreBlank() {
        assertThat(userService.findDTOByUsernameAndEmail(null, "email@test.com")).isEmpty();
        assertThat(userService.findDTOByUsernameAndEmail("user", null)).isEmpty();
        assertThat(userService.findDTOByUsernameAndEmail("  ", "email@test.com")).isEmpty();
        assertThat(userService.findDTOByUsernameAndEmail("user", "  ")).isEmpty();
        verify(userRepository, never()).findByUsernameAndEmail(any(), any());
    }
}
