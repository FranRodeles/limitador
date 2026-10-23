package ar.edu.um.ingenieria.limitador.config;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.UserDTO;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;
import ar.edu.um.ingenieria.limitador.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_PASSWORD = "AdminPass123!";
    private static final String USER_PASSWORD = "UserPass123!";

    private Long adminId;
    private Long userId;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void seedAdminAndUser() {
        Role adminRole = roleRepository.save(role("ROLE_ADMIN", "Administrator"));
        Role userRole = roleRepository.save(role("ROLE_USER", "Standard user"));

        User admin = user("admin.user", "admin@example.com", ADMIN_PASSWORD, adminRole);
        adminId = userRepository.save(admin).getId();

        User regular = user("regular.user", "user@example.com", USER_PASSWORD, userRole);
        userId = userRepository.save(regular).getId();

        adminToken = jwtService.generateToken(admin, false);
        userToken = jwtService.generateToken(regular, false);
    }

    @Test
    @DisplayName("Authorization: anonymous user gets 401 reading a profile by id")
    void shouldReturn401WhenAnonymousReadsProfile() throws Exception {
        mockMvc.perform(get("/api/users/" + userId))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authorization: user role gets 401 'No autorizado' reading a profile by id")
    void shouldReturn401NoAutorizadoWhenUserRoleReadsProfile() throws Exception {
        mockMvc.perform(get("/api/users/" + userId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail", is("No autorizado")));
    }

    @Test
    @DisplayName("Authorization: admin role can read any profile")
    void shouldAllowAdminToReadAnyProfile() throws Exception {
        mockMvc.perform(get("/api/users/" + userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("regular.user")));
    }

    @Test
    @DisplayName("Authorization: anonymous user gets 401 updating a profile by id")
    void shouldReturn401WhenAnonymousUpdatesProfile() throws Exception {
        mockMvc.perform(put("/api/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ownProfilePayload())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authorization: user role gets 401 'No autorizado' updating a profile by id")
    void shouldReturn401NoAutorizadoWhenUserRoleUpdatesProfile() throws Exception {
        mockMvc.perform(put("/api/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ownProfilePayload()))
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail", is("No autorizado")));
    }

    @Test
    @DisplayName("Authorization: admin role can update any profile")
    void shouldAllowAdminToUpdateAnyProfile() throws Exception {
        mockMvc.perform(put("/api/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ownProfilePayload()))
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("regular.user")));
    }

    private UserDTO ownProfilePayload() {
        return UserDTO.builder()
            .username("regular.user")
            .email("user@example.com")
            .password(USER_PASSWORD)
            .firstName("Regular")
            .lastName("User")
            .phoneNumber("555-2222")
            .address("Home Street 5")
            .build();
    }

    @Test
    @DisplayName("Authorization: anonymous user gets 401 on the user data list")
    void shouldReturn401WhenAnonymousAccessesUserDataList() throws Exception {
        mockMvc.perform(get("/api/users-data"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authorization: user role gets 401 'No autorizado' on the user data list")
    void shouldReturn401NoAutorizadoWhenUserRoleAccessesUserDataList() throws Exception {
        mockMvc.perform(get("/api/users-data")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail", is("No autorizado")));
    }

    @Test
    @DisplayName("Authorization: admin role can list user data")
    void shouldAllowAdminOnUserDataList() throws Exception {
        mockMvc.perform(get("/api/users-data")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authorization: anonymous user gets 401 on the role list")
    void shouldReturn401WhenAnonymousAccessesRoleList() throws Exception {
        mockMvc.perform(get("/api/roles"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authorization: user role gets 401 'No autorizado' on the role list")
    void shouldReturn401NoAutorizadoWhenUserRoleAccessesRoleList() throws Exception {
        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail", is("No autorizado")));
    }

    @Test
    @DisplayName("Authorization: admin role can list roles")
    void shouldAllowAdminOnRoleList() throws Exception {
        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authorization: user role gets 401 'No autorizado' on user deletion")
    void shouldReturn401NoAutorizadoWhenUserRoleDeletesUser() throws Exception {
        mockMvc.perform(delete("/api/users/" + adminId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail", is("No autorizado")));
    }

    @Test
    @DisplayName("Authorization: admin role can delete a user")
    void shouldAllowAdminToDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/" + userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Authorization: anonymous user gets 401 on user creation")
    void shouldReturn401WhenAnonymousCreatesUser() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUserPayload())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authorization: user role gets 401 'No autorizado' on user creation")
    void shouldReturn401NoAutorizadoWhenUserRoleCreatesUser() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUserPayload()))
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail", is("No autorizado")));
    }

    @Test
    @DisplayName("Authorization: admin role can create a user")
    void shouldAllowAdminToCreateUser() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUserPayload()))
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username", is("created.by.admin")));
    }

    private UserDTO newUserPayload() {
        return UserDTO.builder()
            .username("created.by.admin")
            .email("created.by.admin@example.com")
            .password("CreatePass123!")
            .activated(true)
            .roles(Set.of("ROLE_USER"))
            .firstName("Created")
            .lastName("ByAdmin")
            .phoneNumber("555-0001")
            .address("Admin Street 1")
            .build();
    }

    @Test
    @DisplayName("Authorization: anonymous user gets 401 on the user list")
    void shouldReturn401WhenAnonymousAccessesUserList() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authorization: user role gets 401 'No autorizado' on the admin user list")
    void shouldReturn401NoAutorizadoWhenUserRoleAccessesUserList() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail", is("No autorizado")));
    }

    @Test
    @DisplayName("Authorization: admin role gets 200 on the admin user search")
    void shouldAllowAdminOnUserSearch() throws Exception {
        mockMvc.perform(get("/api/users/search")
                .param("username", "admin.user")
                .param("email", "admin@example.com")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authorization: user role gets 401 'No autorizado' on the admin user search")
    void shouldReturn401NoAutorizadoWhenUserRoleAccessesUserSearch() throws Exception {
        mockMvc.perform(get("/api/users/search")
                .param("username", "admin.user")
                .param("email", "admin@example.com")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail", is("No autorizado")));
    }

    @Test
    @DisplayName("Authorization: anonymous user gets 401 on any unlisted endpoint")
    void shouldReturn401WhenAnonymousAccessesUnlistedEndpoint() throws Exception {
        mockMvc.perform(get("/api/unlisted-endpoint"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authorization: anonymous user can login")
    void shouldAllowAnonymousLogin() throws Exception {
        var loginPayload = ar.edu.um.ingenieria.limitador.dto.UserLoginDTO.builder()
            .username("regular.user")
            .password(USER_PASSWORD)
            .rememberMe(false)
            .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Authorization: anonymous user can register")
    void shouldAllowAnonymousRegister() throws Exception {
        var registerPayload = ar.edu.um.ingenieria.limitador.dto.UserRegisterDTO.builder()
            .username("anon.register")
            .email("anon.register@example.com")
            .password("RegisterPass123!")
            .firstName("Anon")
            .lastName("Register")
            .phoneNumber("555-9999")
            .address("Public Street 9")
            .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerPayload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username", is("anon.register")))
            .andExpect(jsonPath("$.activated").value(false));
    }

    private Role role(String roleName, String description) {
        Role role = new Role();
        role.setRoleName(roleName);
        role.setDescription(description);
        return role;
    }

    private User user(String username, String email, String rawPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActivated(true);
        user.setRoles(new HashSet<>(Set.of(role)));
        return user;
    }
}
