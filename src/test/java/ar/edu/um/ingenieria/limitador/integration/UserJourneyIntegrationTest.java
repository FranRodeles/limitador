package ar.edu.um.ingenieria.limitador.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import ar.edu.um.ingenieria.limitador.dto.UserDTO;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserJourneyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("User Journey: Complete lifecycle - Register -> Retrieve -> Update -> List -> Delete -> Verify 404")
    void executeCompleteUserJourney() throws Exception {
        // Step 1: User Registration via UserDTO
        UserDTO registrationDto = UserDTO.builder()
            .username("robert_martin")
            .email("unclebob@cleancode.org")
            .password("cleanCode101!")
            .activated(true)
            .firstName("Robert")
            .lastName("Martin")
            .phoneNumber("+1-555-CLEAN")
            .address("Solid Principles Way 42")
            .roles(Set.of("ROLE_AUTHOR", "ROLE_DEVELOPER"))
            .build();

        var registrationResponse = mockMvc.perform(post("/api/users/dto")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.username", is("robert_martin")))
            .andExpect(jsonPath("$.email", is("unclebob@cleancode.org")))
            .andExpect(jsonPath("$.firstName", is("Robert")))
            .andExpect(jsonPath("$.lastName", is("Martin")))
            .andExpect(jsonPath("$.phoneNumber", is("+1-555-CLEAN")))
            .andExpect(jsonPath("$.address", is("Solid Principles Way 42")))
            .andReturn();

        UserDTO createdUser = objectMapper.readValue(
            registrationResponse.getResponse().getContentAsString(),
            UserDTO.class
        );
        Long userId = createdUser.getId();
        assertThat(userId).isNotNull();

        // Step 2: Retrieve Profile via UserDTO
        mockMvc.perform(get("/api/users/dto/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(userId.intValue())))
            .andExpect(jsonPath("$.username", is("robert_martin")))
            .andExpect(jsonPath("$.email", is("unclebob@cleancode.org")))
            .andExpect(jsonPath("$.firstName", is("Robert")));

        // Step 3: Update Profile Information via UserDTO
        UserDTO updateDto = UserDTO.builder()
            .username("robert_c_martin")
            .email("unclebob@cleancoders.com")
            .password("cleanArchitecture2026!")
            .activated(true)
            .firstName("Robert C.")
            .lastName("Martin (Uncle Bob)")
            .phoneNumber("+1-555-CRAFTSMAN")
            .address("Agile Software Boulevard 100")
            .roles(Set.of("ROLE_AUTHOR", "ROLE_SPEAKER"))
            .build();

        mockMvc.perform(put("/api/users/dto/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(userId.intValue())))
            .andExpect(jsonPath("$.username", is("robert_c_martin")))
            .andExpect(jsonPath("$.email", is("unclebob@cleancoders.com")))
            .andExpect(jsonPath("$.firstName", is("Robert C.")))
            .andExpect(jsonPath("$.lastName", is("Martin (Uncle Bob)")))
            .andExpect(jsonPath("$.phoneNumber", is("+1-555-CRAFTSMAN")))
            .andExpect(jsonPath("$.address", is("Agile Software Boulevard 100")));

        // Step 4: Verify user in collection list
        var listResponse = mockMvc.perform(get("/api/users/dto"))
            .andExpect(status().isOk())
            .andReturn();

        String listContent = listResponse.getResponse().getContentAsString();
        assertThat(listContent).contains("robert_c_martin");
        assertThat(listContent).contains("unclebob@cleancoders.com");

        // Step 5: Delete user
        mockMvc.perform(delete("/api/users/" + userId))
            .andExpect(status().isNoContent());

        // Step 6: Verify user is deleted (404 Not Found)
        mockMvc.perform(get("/api/users/dto/" + userId))
            .andExpect(status().isNotFound());
    }
}
