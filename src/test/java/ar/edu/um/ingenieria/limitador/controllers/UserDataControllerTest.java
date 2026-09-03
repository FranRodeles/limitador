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

import ar.edu.um.ingenieria.limitador.domain.UserData;
import ar.edu.um.ingenieria.limitador.services.UserDataService;

@WebMvcTest(UserDataController.class)
class UserDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDataService userDataService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserData createUserData(Long id, String firstName, String lastName, String address, String phoneNumber) {
        UserData ud = new UserData();
        ud.setId(id);
        ud.setFirstName(firstName);
        ud.setLastName(lastName);
        ud.setAddress(address);
        ud.setPhoneNumber(phoneNumber);
        return ud;
    }

    @Test
    void shouldReturnAllUserData() throws Exception {
        var ud1 = createUserData(1L, "Juan", "Perez", "Calle 1", "111");
        var ud2 = createUserData(2L, "Maria", "Lopez", "Calle 2", "222");
        when(userDataService.findAll()).thenReturn(List.of(ud1, ud2));

        mockMvc.perform(get("/api/users-data"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].firstName", is("Juan")))
            .andExpect(jsonPath("$[1].firstName", is("Maria")));
    }

    @Test
    void shouldReturnUserDataById() throws Exception {
        var ud = createUserData(1L, "Juan", "Perez", "Calle 1", "111");
        when(userDataService.findById(1L)).thenReturn(Optional.of(ud));

        mockMvc.perform(get("/api/users-data/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName", is("Juan")));
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        when(userDataService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users-data/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateUserData() throws Exception {
        var ud = createUserData(null, "Carlos", null, null, "333");
        var saved = createUserData(1L, "Carlos", null, null, "333");
        when(userDataService.save(any(UserData.class))).thenReturn(saved);

        mockMvc.perform(post("/api/users-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ud)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.firstName", is("Carlos")));
    }

    @Test
    void shouldUpdateUserData() throws Exception {
        var updated = createUserData(1L, "Updated", "New", "Addr", "555");
        when(userDataService.update(any(Long.class), any(UserData.class))).thenReturn(updated);

        mockMvc.perform(put("/api/users-data/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName", is("Updated")));
    }

    @Test
    void shouldDeleteUserData() throws Exception {
        mockMvc.perform(delete("/api/users-data/1"))
            .andExpect(status().isNoContent());
    }
}
