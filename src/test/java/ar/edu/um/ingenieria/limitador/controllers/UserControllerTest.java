package ar.edu.um.ingenieria.limitador.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

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
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDTO createUserDTO(Long id, String username, String email) {
        return UserDTO.builder()
            .id(id)
            .username(username)
            .email(email)
            .activated(true)
            .build();
    }

    @Test
    void shouldReturnPagedUsers() throws Exception {
        var u1 = createUserDTO(1L, "jdoe", "jdoe@example.com");
        var u2 = createUserDTO(2L, "jane", "jane@example.com");
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserDTO> page = new PageImpl<>(List.of(u1, u2), pageable, 2);
        when(userService.findAllDTOs(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users?page=0&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].username", is("jdoe")))
            .andExpect(jsonPath("$.content[1].username", is("jane")));
    }

    @Test
    void shouldReturnUserById() throws Exception {
        var user = createUserDTO(1L, "jdoe", "jdoe@example.com");
        when(userService.findDTOById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("jdoe")));
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        when(userService.findDTOById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateUser() throws Exception {
        var user = createUserDTO(null, "newuser", "new@example.com");
        var saved = createUserDTO(1L, "newuser", "new@example.com");
        when(userService.saveDTO(any(UserDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.username", is("newuser")));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        var updated = createUserDTO(1L, "updated", "updated@example.com");
        when(userService.updateDTO(eq(1L), any(UserDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("updated")));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnUserByUsernameAndEmail() throws Exception {
        var user = createUserDTO(1L, "jdoe", "jdoe@example.com");
        when(userService.findDTOByUsernameAndEmail("jdoe", "jdoe@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/search?username=jdoe&email=jdoe@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.username", is("jdoe")))
            .andExpect(jsonPath("$.email", is("jdoe@example.com")));
    }

    @Test
    void shouldReturn404WhenUserNotFoundByUsernameAndEmail() throws Exception {
        when(userService.findDTOByUsernameAndEmail("unknown", "unknown@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/search?username=unknown&email=unknown@example.com"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenSearchParametersAreBlank() throws Exception {
        mockMvc.perform(get("/api/users/search?username=  &email=jdoe@example.com"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/users/search?username=jdoe&email=  "))
            .andExpect(status().isBadRequest());
    }
}
