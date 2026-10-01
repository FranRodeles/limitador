package ar.edu.um.ingenieria.limitador.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import ar.edu.um.ingenieria.limitador.dto.UserDTO;
import ar.edu.um.ingenieria.limitador.services.UserService;

@WebMvcTest(UserController.class)
class UserDtoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/users should return paged UserDTOs")
    void shouldReturnPagedUserDTOs() throws Exception {
        UserDTO dto1 = UserDTO.builder().id(1L).username("alice").email("alice@example.com").build();
        UserDTO dto2 = UserDTO.builder().id(2L).username("bob").email("bob@example.com").build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserDTO> page = new PageImpl<>(List.of(dto1, dto2), pageable, 2);
        when(userService.findAllDTOs(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users?page=0&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].username", is("alice")))
            .andExpect(jsonPath("$.content[1].username", is("bob")));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return UserDTO when exists")
    void shouldReturnUserDTOById() throws Exception {
        UserDTO dto = UserDTO.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .firstName("Alice")
            .roles(Set.of("ROLE_USER"))
            .build();
        when(userService.findDTOById(1L)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.username", is("alice")))
            .andExpect(jsonPath("$.firstName", is("Alice")));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return 404 when not found")
    void shouldReturn404WhenUserDTONotFound() throws Exception {
        when(userService.findDTOById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/users should create and return UserDTO with 201 Created")
    void shouldCreateUserDTO() throws Exception {
        UserDTO requestDto = UserDTO.builder()
            .username("carol")
            .email("carol@example.com")
            .password("secretCarol")
            .activated(true)
            .firstName("Carol")
            .lastName("Danvers")
            .phoneNumber("555-9999")
            .build();

        UserDTO responseDto = UserDTO.builder()
            .id(10L)
            .username("carol")
            .email("carol@example.com")
            .activated(true)
            .firstName("Carol")
            .lastName("Danvers")
            .phoneNumber("555-9999")
            .build();

        when(userService.saveDTO(any(UserDTO.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(10)))
            .andExpect(jsonPath("$.username", is("carol")))
            .andExpect(jsonPath("$.firstName", is("Carol")))
            .andExpect(jsonPath("$.phoneNumber", is("555-9999")));
    }

    @Test
    @DisplayName("PUT /api/users/{id} should update and return UserDTO with 200 OK")
    void shouldUpdateUserDTO() throws Exception {
        UserDTO updateDto = UserDTO.builder()
            .username("alice_updated")
            .email("alice_new@example.com")
            .firstName("Alicia")
            .build();

        UserDTO responseDto = UserDTO.builder()
            .id(1L)
            .username("alice_updated")
            .email("alice_new@example.com")
            .firstName("Alicia")
            .build();

        when(userService.updateDTO(eq(1L), any(UserDTO.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.username", is("alice_updated")))
            .andExpect(jsonPath("$.firstName", is("Alicia")));
    }
}
