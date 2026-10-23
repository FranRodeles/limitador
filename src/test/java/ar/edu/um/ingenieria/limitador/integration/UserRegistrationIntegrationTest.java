package ar.edu.um.ingenieria.limitador.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.UserDTO;
import ar.edu.um.ingenieria.limitador.dto.UserRegisterDTO;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Integration: Register user - verify DB state, activated=false, encrypted password, and UserData")
    void shouldRegisterUserAndPersistWithActivatedFalse() throws Exception {
        UserRegisterDTO registrationDto = UserRegisterDTO.builder()
            .username("ada_lovelace")
            .email("ada@analyticalengine.org")
            .password("firstProgrammer1843!")
            .firstName("Ada")
            .lastName("Lovelace")
            .address("London, UK")
            .phoneNumber("+44 20 7946 0991")
            .build();

        var mvcResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.username", is("ada_lovelace")))
            .andExpect(jsonPath("$.email", is("ada@analyticalengine.org")))
            .andExpect(jsonPath("$.activated", is(false)))
            .andExpect(jsonPath("$.firstName", is("Ada")))
            .andExpect(jsonPath("$.lastName", is("Lovelace")))
            .andExpect(jsonPath("$.address", is("London, UK")))
            .andExpect(jsonPath("$.phoneNumber", is("+44 20 7946 0991")))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andReturn();

        UserDTO responseDto = objectMapper.readValue(
            mvcResult.getResponse().getContentAsString(),
            UserDTO.class
        );

        Long createdId = responseDto.getId();
        assertThat(createdId).isNotNull();

        // Verify Database persistence
        User persistedUser = userRepository.findById(createdId).orElseThrow();
        assertThat(persistedUser.getUsername()).isEqualTo("ada_lovelace");
        assertThat(persistedUser.getEmail()).isEqualTo("ada@analyticalengine.org");
        assertThat(persistedUser.getActivated()).isFalse(); // Business rule: inactive upon registration
        assertThat(passwordEncoder.matches("firstProgrammer1843!", persistedUser.getPassword())).isTrue();
        assertThat(persistedUser.getPassword()).isNotEqualTo("firstProgrammer1843!");

        // Verify UserData relationship
        assertThat(persistedUser.getUserData()).isNotNull();
        assertThat(persistedUser.getUserData().getFirstName()).isEqualTo("Ada");
        assertThat(persistedUser.getUserData().getLastName()).isEqualTo("Lovelace");
        assertThat(persistedUser.getUserData().getAddress()).isEqualTo("London, UK");
        assertThat(persistedUser.getUserData().getPhoneNumber()).isEqualTo("+44 20 7946 0991");
    }

    @Test
    @DisplayName("Integration: Register user without phone number should fail with 400 Bad Request")
    void shouldFailRegistrationWhenPhoneNumberIsMissing() throws Exception {
        UserRegisterDTO registrationDto = UserRegisterDTO.builder()
            .username("charles_babbage")
            .email("babbage@differenceengine.org")
            .password("differenceEngine1822!")
            .firstName("Charles")
            .lastName("Babbage")
            .address("London, UK")
            .phoneNumber(null)
            .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration: Register user with duplicate username should fail with 409 Conflict ProblemDetail")
    void shouldFailRegistrationWhenUsernameAlreadyExists() throws Exception {
        UserRegisterDTO firstDto = UserRegisterDTO.builder()
            .username("duplicate_user")
            .email("user1@example.com")
            .password("Password123!")
            .firstName("First")
            .phoneNumber("+123456789")
            .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstDto)))
            .andExpect(status().isCreated());

        UserRegisterDTO duplicateUsernameDto = UserRegisterDTO.builder()
            .username("duplicate_user")
            .email("user2@example.com")
            .password("Password123!")
            .firstName("Second")
            .phoneNumber("+987654321")
            .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUsernameDto)))
            .andExpect(status().isConflict())
            .andExpect(result -> {
                String contentType = result.getResponse().getContentType();
                assertThat(contentType).containsAnyOf(
                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE
                );
            })
            .andExpect(jsonPath("$.status", is(409)))
            .andExpect(jsonPath("$.title", is("User Already Exists")))
            .andExpect(jsonPath("$.detail", is("Username 'duplicate_user' is already registered")));
    }

    @Test
    @DisplayName("Integration: Register user with duplicate email should fail with 409 Conflict ProblemDetail")
    void shouldFailRegistrationWhenEmailAlreadyExists() throws Exception {
        UserRegisterDTO firstDto = UserRegisterDTO.builder()
            .username("user_one")
            .email("duplicate@example.com")
            .password("Password123!")
            .firstName("First")
            .phoneNumber("+123456789")
            .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstDto)))
            .andExpect(status().isCreated());

        UserRegisterDTO duplicateEmailDto = UserRegisterDTO.builder()
            .username("user_two")
            .email("duplicate@example.com")
            .password("Password123!")
            .firstName("Second")
            .phoneNumber("+987654321")
            .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateEmailDto)))
            .andExpect(status().isConflict())
            .andExpect(result -> {
                String contentType = result.getResponse().getContentType();
                assertThat(contentType).containsAnyOf(
                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE
                );
            })
            .andExpect(jsonPath("$.status", is(409)))
            .andExpect(jsonPath("$.title", is("User Already Exists")))
            .andExpect(jsonPath("$.detail", is("Email 'duplicate@example.com' is already registered")));
    }
}
