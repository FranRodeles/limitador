package ar.edu.um.ingenieria.limitador.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.UserLoginDTO;
import ar.edu.um.ingenieria.limitador.dto.UserLoginResponseDTO;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;
import ar.edu.um.ingenieria.limitador.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private static final String RAW_PASSWORD = "CorrectPassword123!";

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRoleName("ROLE_USER");
        role.setDescription("Standard user");
        Role savedRole = roleRepository.save(role);

        testUser = new User();
        testUser.setUsername("testloginuser");
        testUser.setEmail("login@example.com");
        testUser.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        testUser.setActivated(true);
        testUser.setRoles(Set.of(savedRole));
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Integration 1: Login with correct credentials returns 200 OK, valid JWT, and success message")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("testloginuser")
            .password(RAW_PASSWORD)
            .rememberMe(false)
            .build();

        var result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.message", is("Inicio de sesión exitoso")))
            .andReturn();

        UserLoginResponseDTO response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            UserLoginResponseDTO.class
        );

        assertThat(response.getToken()).isNotBlank();
        assertThat(jwtService.validateToken(response.getToken())).isTrue();
        assertThat(jwtService.isTokenValid(response.getToken(), testUser)).isTrue();
        assertThat(jwtService.extractUsername(response.getToken())).isEqualTo("testloginuser");
    }

    @Test
    @DisplayName("Integration 2: Login with non-existing username returns 401 Unauthorized with RFC 9457 ProblemDetail")
    void shouldReturn401ProblemDetailWhenUsernameDoesNotExist() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("non_existing_user")
            .password(RAW_PASSWORD)
            .rememberMe(false)
            .build();

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isUnauthorized())
            .andExpect(result -> {
                String contentType = result.getResponse().getContentType();
                assertThat(contentType).containsAnyOf(
                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE
                );
            })
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.title", is("Unauthorized")))
            .andExpect(jsonPath("$.detail", is("Error de usuario y/o de password")));
    }

    @Test
    @DisplayName("Integration 3: Login with incorrect password returns 401 Unauthorized without revealing credential details")
    void shouldReturn401ProblemDetailWhenPasswordIsIncorrect() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("testloginuser")
            .password("WrongPassword999!")
            .rememberMe(false)
            .build();

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.title", is("Unauthorized")))
            .andExpect(jsonPath("$.detail", is("Error de usuario y/o de password")));
    }

    @Test
    @DisplayName("Integration 4: Login with rememberMe = false generates token with normal expiration (24 hours)")
    void shouldGenerateNormalExpirationTokenWhenRememberMeIsFalse() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("testloginuser")
            .password(RAW_PASSWORD)
            .rememberMe(false)
            .build();

        Instant before = Instant.now();
        var result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isOk())
            .andReturn();

        UserLoginResponseDTO response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            UserLoginResponseDTO.class
        );

        Date exp = jwtService.extractExpiration(response.getToken());
        Instant expectedMin = before.plus(23, ChronoUnit.HOURS);
        Instant expectedMax = before.plus(25, ChronoUnit.HOURS);

        assertThat(exp.toInstant()).isBetween(expectedMin, expectedMax);
    }

    @Test
    @DisplayName("Integration 5: Login with rememberMe = true generates token with 1 year expiration")
    void shouldGenerateOneYearExpirationTokenWhenRememberMeIsTrue() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("testloginuser")
            .password(RAW_PASSWORD)
            .rememberMe(true)
            .build();

        Instant before = Instant.now();
        var result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isOk())
            .andReturn();

        UserLoginResponseDTO response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            UserLoginResponseDTO.class
        );

        Date exp = jwtService.extractExpiration(response.getToken());
        Instant expectedMin = before.plus(364, ChronoUnit.DAYS);
        Instant expectedMax = before.plus(366, ChronoUnit.DAYS);

        assertThat(exp.toInstant()).isBetween(expectedMin, expectedMax);
    }

    @Test
    @DisplayName("Integration 6: JWT payload contains ONLY username as business claim and exp (no sub, iss, aud, iat, roles, id, etc.)")
    void shouldVerifyJwtContainsOnlyUsernameAndExp() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("testloginuser")
            .password(RAW_PASSWORD)
            .build();

        var result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isOk())
            .andReturn();

        UserLoginResponseDTO response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            UserLoginResponseDTO.class
        );

        String[] parts = response.getToken().split("\\.");
        assertThat(parts).hasSize(3);

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

        assertThat(claims).containsOnlyKeys("username", "exp");
        assertThat(claims.get("username")).isEqualTo("testloginuser");
        assertThat(claims).doesNotContainKeys("sub", "iss", "aud", "iat", "jti", "roles", "id", "password", "email");
    }

    @Test
    @DisplayName("Integration 7: Tampered token must be rejected")
    void shouldRejectTamperedToken() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("testloginuser")
            .password(RAW_PASSWORD)
            .build();

        var result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isOk())
            .andReturn();

        UserLoginResponseDTO response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            UserLoginResponseDTO.class
        );

        String token = response.getToken();
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidSignature";

        assertThat(jwtService.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("Integration 8: Inactive user (activated=false) must be rejected with 401 ProblemDetail")
    void shouldRejectInactiveUser() throws Exception {
        testUser.setActivated(false);
        userRepository.save(testUser);

        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("testloginuser")
            .password(RAW_PASSWORD)
            .build();

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.detail", is("Error de usuario y/o de password")));
    }

    @Test
    @DisplayName("Integration 9: Valid JWT token in Authorization header passes through security filter")
    void shouldAuthenticateRequestWithBearerToken() throws Exception {
        String token = jwtService.generateToken(testUser, false);

        mockMvc.perform(get("/api/users/" + testUser.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
}
