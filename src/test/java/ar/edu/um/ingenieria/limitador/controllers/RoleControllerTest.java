package ar.edu.um.ingenieria.limitador.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.services.RoleService;

@WebMvcTest(RoleController.class)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @Autowired
    private ObjectMapper objectMapper;

    private Role createRole(Long id, String description, String roleName) {
        Role role = new Role();
        role.setId(id);
        role.setDescription(description);
        role.setRoleName(roleName);
        return role;
    }

    @Test
    void shouldReturnAllRoles() throws Exception {
        var role1 = createRole(1L, "Admin", "ROLE_ADMIN");
        var role2 = createRole(2L, "User", "ROLE_USER");
        when(roleService.findAll()).thenReturn(List.of(role1, role2));

        mockMvc.perform(get("/api/roles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].roleName", is("ROLE_ADMIN")))
            .andExpect(jsonPath("$[1].roleName", is("ROLE_USER")));
    }

    @Test
    void shouldReturnRoleById() throws Exception {
        var role = createRole(1L, "Admin", "ROLE_ADMIN");
        when(roleService.findById(1L)).thenReturn(Optional.of(role));

        mockMvc.perform(get("/api/roles/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleName", is("ROLE_ADMIN")));
    }

    @Test
    void shouldReturn404WhenRoleNotFound() throws Exception {
        when(roleService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/roles/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateRole() throws Exception {
        var role = createRole(null, "Editor", "ROLE_EDITOR");
        var saved = createRole(1L, "Editor", "ROLE_EDITOR");
        when(roleService.save(any(Role.class))).thenReturn(saved);

        mockMvc.perform(post("/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(role)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.roleName", is("ROLE_EDITOR")));
    }

    @Test
    void shouldUpdateRole() throws Exception {
        var updated = createRole(1L, "Updated", "ROLE_UPDATED");
        when(roleService.update(any(Long.class), any(Role.class))).thenReturn(updated);

        mockMvc.perform(put("/api/roles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleName", is("ROLE_UPDATED")));
    }

    @Test
    void shouldDeleteRole() throws Exception {
        mockMvc.perform(delete("/api/roles/1"))
            .andExpect(status().isNoContent());
    }
}
