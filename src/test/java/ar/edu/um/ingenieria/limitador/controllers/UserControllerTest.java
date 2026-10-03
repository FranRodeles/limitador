package ar.edu.um.ingenieria.limitador.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.mockito.ArgumentCaptor;
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
    @DisplayName("GET /api/users?page=0&size=20 should return paged users")
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
    @DisplayName("GET /api/users?page=20&size=20 should pass page=20 and size=20 to service")
    void shouldPassRequestedPageAndSizeToService() throws Exception {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        Page<UserDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(20, 20), 0);
        when(userService.findAllDTOs(captor.capture())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/users?page=20&size=20"))
            .andExpect(status().isOk());

        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(20);
        assertThat(captured.getPageSize()).isEqualTo(20);
        verify(userService).findAllDTOs(captured);
    }

    @Test
    @DisplayName("GET /api/users without params should apply default pagination (page=0, size=20)")
    void shouldApplyDefaultPaginationWhenNoParamsProvided() throws Exception {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        Page<UserDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(userService.findAllDTOs(captor.capture())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk());

        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isZero();
        assertThat(captured.getPageSize()).isEqualTo(20);
        verify(userService).findAllDTOs(captured);
    }

    @Test
    @DisplayName("GET /api/users?page=20&size=10&sort=username,desc should pass page, size and sort")
    void shouldSupportSortingWithPagination() throws Exception {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        Page<UserDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(20, 10), 0);
        when(userService.findAllDTOs(captor.capture())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/users?page=20&size=10&sort=username,desc"))
            .andExpect(status().isOk());

        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(20);
        assertThat(captured.getPageSize()).isEqualTo(10);
        assertThat(captured.getSort().getOrderFor("username")).isNotNull();
        assertThat(captured.getSort().getOrderFor("username").isDescending()).isTrue();
        verify(userService).findAllDTOs(captured);
    }

    @Test
    @DisplayName("GET /api/users/{id} should return user when exists")
    void shouldReturnUserById() throws Exception {
        var user = UserDTO.builder()
            .id(1L)
            .username("jdoe")
            .email("jdoe@example.com")
            .firstName("John")
            .roles(Set.of("ROLE_USER"))
            .activated(true)
            .build();
        when(userService.findDTOById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.username", is("jdoe")))
            .andExpect(jsonPath("$.firstName", is("John")));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return 404 when not found")
    void shouldReturn404WhenNotFound() throws Exception {
        when(userService.findDTOById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/users should create and return user with 201 Created")
    void shouldCreateUser() throws Exception {
        var user = UserDTO.builder()
            .username("newuser")
            .email("new@example.com")
            .password("secretPass")
            .firstName("New")
            .phoneNumber("555-1234")
            .activated(true)
            .build();
        var saved = UserDTO.builder()
            .id(1L)
            .username("newuser")
            .email("new@example.com")
            .firstName("New")
            .phoneNumber("555-1234")
            .activated(true)
            .build();
        when(userService.saveDTO(any(UserDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.username", is("newuser")))
            .andExpect(jsonPath("$.firstName", is("New")))
            .andExpect(jsonPath("$.phoneNumber", is("555-1234")));
    }

    @Test
    @DisplayName("PUT /api/users/{id} should update and return user with 200 OK")
    void shouldUpdateUser() throws Exception {
        var updated = UserDTO.builder()
            .username("updated")
            .email("updated@example.com")
            .firstName("UpdatedName")
            .activated(true)
            .build();
        var response = UserDTO.builder()
            .id(1L)
            .username("updated")
            .email("updated@example.com")
            .firstName("UpdatedName")
            .activated(true)
            .build();
        when(userService.updateDTO(eq(1L), any(UserDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.username", is("updated")))
            .andExpect(jsonPath("$.firstName", is("UpdatedName")));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} should delete user with 204 No Content")
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
