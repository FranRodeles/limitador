package ar.edu.um.ingenieria.limitador.system;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
class UserSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("System: should create User via POST /api/users/dto and retrieve it via GET")
    void shouldCreateAndRetrieveUserViaDtoEndpoints() throws Exception {
        UserDTO createDto = UserDTO.builder()
            .username("systemUser")
            .email("system@enterprise.com")
            .password("systemPass123")
            .activated(true)
            .firstName("System")
            .lastName("Operator")
            .phoneNumber("111-222333")
            .address("Tech Park 100")
            .roles(Set.of("ROLE_ADMIN"))
            .build();

        // 1. POST /api/users/dto -> 201 Created
        var postResult = mockMvc.perform(post("/api/users/dto")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.username", is("systemUser")))
            .andExpect(jsonPath("$.email", is("system@enterprise.com")))
            .andExpect(jsonPath("$.firstName", is("System")))
            .andExpect(jsonPath("$.phoneNumber", is("111-222333")))
            .andReturn();

        UserDTO created = objectMapper.readValue(postResult.getResponse().getContentAsString(), UserDTO.class);

        // 2. GET /api/users/dto/{id} -> 200 OK
        mockMvc.perform(get("/api/users/dto/" + created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(created.getId().intValue())))
            .andExpect(jsonPath("$.username", is("systemUser")))
            .andExpect(jsonPath("$.email", is("system@enterprise.com")))
            .andExpect(jsonPath("$.firstName", is("System")))
            .andExpect(jsonPath("$.lastName", is("Operator")))
            .andExpect(jsonPath("$.address", is("Tech Park 100")));
    }

    @Test
    @DisplayName("System: should return 404 when querying non-existent UserDTO")
    void shouldReturn404ForNonExistentUserDTO() throws Exception {
        mockMvc.perform(get("/api/users/dto/99999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("System: should update existing user via PUT /api/users/dto/{id}")
    void shouldUpdateUserViaDtoEndpoint() throws Exception {
        UserDTO createDto = UserDTO.builder()
            .username("initialUser")
            .email("initial@enterprise.com")
            .password("pass123")
            .activated(true)
            .firstName("Initial")
            .phoneNumber("999-000")
            .build();

        var postResult = mockMvc.perform(post("/api/users/dto")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andReturn();

        UserDTO created = objectMapper.readValue(postResult.getResponse().getContentAsString(), UserDTO.class);

        UserDTO updateDto = UserDTO.builder()
            .username("modifiedUser")
            .email("modified@enterprise.com")
            .password("newPass456")
            .activated(false)
            .firstName("Modified")
            .lastName("User")
            .phoneNumber("888-111")
            .address("New Address 200")
            .build();

        mockMvc.perform(put("/api/users/dto/" + created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(created.getId().intValue())))
            .andExpect(jsonPath("$.username", is("modifiedUser")))
            .andExpect(jsonPath("$.email", is("modified@enterprise.com")))
            .andExpect(jsonPath("$.firstName", is("Modified")))
            .andExpect(jsonPath("$.activated", is(false)));
    }
}
