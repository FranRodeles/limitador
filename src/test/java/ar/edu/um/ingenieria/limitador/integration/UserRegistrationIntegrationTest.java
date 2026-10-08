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

        var mvcResult = mockMvc.perform(post("/api/users/register")
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

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
            .andExpect(status().isBadRequest());
    }
}
