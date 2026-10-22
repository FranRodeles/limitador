package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.dto.UserDTO;
import ar.edu.um.ingenieria.limitador.dto.UserRegisterDTO;
import ar.edu.um.ingenieria.limitador.exception.UserAlreadyExistsException;
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

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Mock
    private ar.edu.um.ingenieria.limitador.security.JwtService jwtService;

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
    @DisplayName("Should reuse a role created concurrently instead of failing with a constraint violation")
    void shouldReuseRoleCreatedConcurrently() {
        UserDTO inputDto = UserDTO.builder()
            .username("roleRace")
            .email("roleRace@test.com")
            .password("secret")
            .activated(true)
            .roles(Set.of("ROLE_NEW"))
            .build();

        Role requestedRole = new Role();
        requestedRole.setRoleName("ROLE_NEW");
        requestedRole.setDescription("New role");

        Role existingRole = new Role();
        existingRole.setId(7L);
        existingRole.setRoleName("ROLE_NEW");
        existingRole.setDescription("New role");

        User entityToSave = createUser(null, "roleRace", "roleRace@test.com", "secret", true);
        entityToSave.setRoles(new HashSet<>(Set.of(requestedRole)));

        User savedEntity = createUser(11L, "roleRace", "roleRace@test.com", "$2a$10$hash", true);
        UserDTO expectedDto = UserDTO.builder()
            .id(11L)
            .username("roleRace")
            .email("roleRace@test.com")
            .activated(true)
            .roles(Set.of("ROLE_NEW"))
            .build();

        when(userMapper.toEntity(inputDto)).thenReturn(entityToSave);
        when(passwordEncoder.encode("secret")).thenReturn("$2a$10$hash");
        when(roleRepository.findByRoleName("ROLE_NEW"))
            .thenReturn(Optional.empty(), Optional.of(existingRole));
        when(roleRepository.save(any(Role.class)))
            .thenThrow(new DataIntegrityViolationException("roles_role_name_key"));
        when(userRepository.save(entityToSave)).thenReturn(savedEntity);
        when(userMapper.toDto(savedEntity)).thenReturn(expectedDto);

        UserDTO result = userService.saveDTO(inputDto);

        assertThat(result.getId()).isEqualTo(11L);
        assertThat(entityToSave.getRoles()).containsExactly(existingRole);
        verify(roleRepository).save(any(Role.class));
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
    @DisplayName("Should default activated to false when UserDTO omits it")
    void shouldDefaultActivatedToFalseWhenDtoOmitsIt() {
        UserDTO inputDto = UserDTO.builder()
            .username("noFlag")
            .email("noFlag@test.com")
            .password("secret")
            .build();

        User entityToSave = createUser(null, "noFlag", "noFlag@test.com", "secret", null);
        User savedEntity = createUser(12L, "noFlag", "noFlag@test.com", "$2a$10$hash", false);
        UserDTO expectedDto = UserDTO.builder()
            .id(12L)
            .username("noFlag")
            .email("noFlag@test.com")
            .activated(false)
            .build();

        when(userMapper.toEntity(inputDto)).thenReturn(entityToSave);
        when(passwordEncoder.encode("secret")).thenReturn("$2a$10$hash");
        when(userRepository.save(entityToSave)).thenReturn(savedEntity);
        when(userMapper.toDto(savedEntity)).thenReturn(expectedDto);

        userService.saveDTO(inputDto);

        assertThat(entityToSave.getActivated()).isFalse();
    }

    @Test
    @DisplayName("Should keep the stored activation flag when UserDTO omits it on update")
    void shouldKeepStoredActivationFlagWhenDtoOmitsItOnUpdate() {
        UserDTO updateDto = UserDTO.builder()
            .username("updatedUser")
            .email("updated@test.com")
            .password("newPass")
            .build();

        User existingUser = createUser(5L, "oldUser", "old@test.com", "oldPass", true);
        User mappedUser = createUser(null, "updatedUser", "updated@test.com", "newPass", null);
        User savedUser = createUser(5L, "updatedUser", "updated@test.com", "$2a$10$hash", true);
        UserDTO resultDto = UserDTO.builder()
            .id(5L)
            .username("updatedUser")
            .email("updated@test.com")
            .activated(true)
            .build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(existingUser));
        when(userMapper.toEntity(updateDto)).thenReturn(mappedUser);
        when(passwordEncoder.encode("newPass")).thenReturn("$2a$10$hash");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(resultDto);

        userService.updateDTO(5L, updateDto);

        assertThat(mappedUser.getActivated()).isTrue();
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

        when(userRepository.findByUsernameOrEmail("searchUser", "search@test.com")).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dtoWithPassword);

        Optional<UserDTO> result = userService.findDTOByUsernameOrEmail("searchUser", "search@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getUsername()).isEqualTo("searchUser");
        assertThat(result.get().getEmail()).isEqualTo("search@test.com");
        assertThat(result.get().getPassword()).isNull();
        verify(userRepository, times(1)).findByUsernameOrEmail("searchUser", "search@test.com");
    }

    @Test
    @DisplayName("Should return empty Optional when UserDTO not found by username and email")
    void shouldReturnEmptyWhenUserDTONotFoundByUsernameAndEmail() {
        when(userRepository.findByUsernameOrEmail("unknown", "unknown@test.com")).thenReturn(Optional.empty());

        Optional<UserDTO> result = userService.findDTOByUsernameOrEmail("unknown", "unknown@test.com");

        assertThat(result).isEmpty();
        verify(userRepository, times(1)).findByUsernameOrEmail("unknown", "unknown@test.com");
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should return empty Optional when search parameters are null or blank")
    void shouldReturnEmptyWhenSearchParametersAreBlank() {
        assertThat(userService.findDTOByUsernameOrEmail(null, "email@test.com")).isEmpty();
        assertThat(userService.findDTOByUsernameOrEmail("user", null)).isEmpty();
        assertThat(userService.findDTOByUsernameOrEmail("  ", "email@test.com")).isEmpty();
        assertThat(userService.findDTOByUsernameOrEmail("user", "  ")).isEmpty();
        verify(userRepository, never()).findByUsernameOrEmail(any(), any());
    }

    @Test
    @DisplayName("Should register new user with activated false and encrypted password")
    void shouldRegisterUserWithActivatedFalseAndEncryptedPassword() {
        UserRegisterDTO registerDto = UserRegisterDTO.builder()
            .username("johndoe")
            .email("johndoe@example.com")
            .password("rawPassword123")
            .firstName("John")
            .lastName("Doe")
            .address("742 Evergreen Terrace")
            .phoneNumber("555-1234")
            .build();

        UserData userData = new UserData();
        userData.setFirstName("John");
        userData.setLastName("Doe");
        userData.setAddress("742 Evergreen Terrace");
        userData.setPhoneNumber("555-1234");

        User entityToSave = createUser(null, "johndoe", "johndoe@example.com", "rawPassword123", false);
        entityToSave.setUserData(userData);

        User savedEntity = createUser(100L, "johndoe", "johndoe@example.com", "$2a$10$hashedPassword", false);
        savedEntity.setUserData(userData);

        UserDTO expectedDto = UserDTO.builder()
            .id(100L)
            .username("johndoe")
            .email("johndoe@example.com")
            .activated(false)
            .firstName("John")
            .lastName("Doe")
            .address("742 Evergreen Terrace")
            .phoneNumber("555-1234")
            .build();

        when(userMapper.toEntity(registerDto)).thenReturn(entityToSave);
        when(passwordEncoder.encode("rawPassword123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(entityToSave)).thenReturn(savedEntity);
        when(userMapper.toDto(savedEntity)).thenReturn(expectedDto);

        UserDTO result = userService.register(registerDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getEmail()).isEqualTo("johndoe@example.com");
        assertThat(result.getActivated()).isFalse();
        assertThat(result.getPassword()).isNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getAddress()).isEqualTo("742 Evergreen Terrace");
        assertThat(result.getPhoneNumber()).isEqualTo("555-1234");

        verify(passwordEncoder).encode("rawPassword123");
        verify(userRepository).save(entityToSave);
        assertThat(entityToSave.getActivated()).isFalse();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when register payload is null")
    void shouldThrowExceptionWhenRegisterPayloadIsNull() {
        assertThatThrownBy(() -> userService.register(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when username already exists")
    void shouldThrowWhenRegisteringExistingUsername() {
        UserRegisterDTO registerDto = UserRegisterDTO.builder()
            .username("existingUser")
            .email("new@example.com")
            .password("password123")
            .firstName("First")
            .phoneNumber("+123456")
            .build();

        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerDto))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessage("Username 'existingUser' is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when email already exists")
    void shouldThrowWhenRegisteringExistingEmail() {
        UserRegisterDTO registerDto = UserRegisterDTO.builder()
            .username("newUser")
            .email("existing@example.com")
            .password("password123")
            .firstName("First")
            .phoneNumber("+123456")
            .build();

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerDto))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessage("Email 'existing@example.com' is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should translate uniqueness violation from a concurrent registration into UserAlreadyExistsException")
    void shouldTranslateConcurrentDuplicateRegistrationIntoUserAlreadyExistsException() {
        UserRegisterDTO registerDto = UserRegisterDTO.builder()
            .username("raceUser")
            .email("race@example.com")
            .password("password123")
            .firstName("Race")
            .phoneNumber("+123456")
            .build();

        User entityToSave = createUser(null, "raceUser", "race@example.com", "password123", false);

        when(userRepository.existsByUsername("raceUser")).thenReturn(false, true);
        when(userRepository.existsByEmail("race@example.com")).thenReturn(false);
        when(userMapper.toEntity(registerDto)).thenReturn(entityToSave);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hash");
        when(userRepository.save(entityToSave))
            .thenThrow(new DataIntegrityViolationException("users_username_key"));

        assertThatThrownBy(() -> userService.register(registerDto))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessage("Username 'raceUser' is already registered");

        verify(userRepository).save(entityToSave);
    }

    @Test
    @DisplayName("Should authenticate and return JWT token with success message when login credentials are valid")
    void shouldAuthenticateAndReturnJwtWhenLoginCredentialsAreValid() {
        ar.edu.um.ingenieria.limitador.dto.UserLoginDTO loginDTO = ar.edu.um.ingenieria.limitador.dto.UserLoginDTO.builder()
            .username("johndoe")
            .password("secret123")
            .rememberMe(false)
            .build();

        User user = createUser(1L, "johndoe", "john@example.com", "hashed", true);
        org.springframework.security.core.Authentication authResult =
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        when(authenticationManager.authenticate(any(org.springframework.security.core.Authentication.class)))
            .thenReturn(authResult);
        when(jwtService.generateToken(user, false)).thenReturn("mocked.jwt.token");

        ar.edu.um.ingenieria.limitador.dto.UserLoginResponseDTO response = userService.login(loginDTO);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getMessage()).isEqualTo("Inicio de sesión exitoso");
        verify(authenticationManager).authenticate(any(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(user, false);
    }

    @Test
    @DisplayName("Should pass rememberMe true to JwtService when rememberMe is true")
    void shouldPassRememberMeTrueToJwtService() {
        ar.edu.um.ingenieria.limitador.dto.UserLoginDTO loginDTO = ar.edu.um.ingenieria.limitador.dto.UserLoginDTO.builder()
            .username("johndoe")
            .password("secret123")
            .rememberMe(true)
            .build();

        User user = createUser(1L, "johndoe", "john@example.com", "hashed", true);
        org.springframework.security.core.Authentication authResult =
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        when(authenticationManager.authenticate(any(org.springframework.security.core.Authentication.class)))
            .thenReturn(authResult);
        when(jwtService.generateToken(user, true)).thenReturn("mocked.longlived.token");

        ar.edu.um.ingenieria.limitador.dto.UserLoginResponseDTO response = userService.login(loginDTO);

        assertThat(response.getToken()).isEqualTo("mocked.longlived.token");
        verify(jwtService).generateToken(user, true);
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException with 'Error de usuario y/o de password' when authentication fails")
    void shouldPropagateBadCredentialsExceptionWhenAuthFails() {
        ar.edu.um.ingenieria.limitador.dto.UserLoginDTO loginDTO = ar.edu.um.ingenieria.limitador.dto.UserLoginDTO.builder()
            .username("wronguser")
            .password("wrongpassword")
            .build();

        when(authenticationManager.authenticate(any(org.springframework.security.core.Authentication.class)))
            .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> userService.login(loginDTO))
            .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
            .hasMessage("Error de usuario y/o de password");

        verify(jwtService, never()).generateToken(any(org.springframework.security.core.userdetails.UserDetails.class), any(boolean.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when login DTO is null")
    void shouldThrowExceptionWhenLoginDtoIsNull() {
        assertThatThrownBy(() -> userService.login(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
