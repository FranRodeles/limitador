package ar.edu.um.ingenieria.limitador.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import ar.edu.um.ingenieria.limitador.dto.UserDTO;
import ar.edu.um.ingenieria.limitador.dto.UserLoginDTO;
import ar.edu.um.ingenieria.limitador.dto.UserLoginResponseDTO;
import ar.edu.um.ingenieria.limitador.dto.UserRegisterDTO;
import ar.edu.um.ingenieria.limitador.exception.UserAlreadyExistsException;
import ar.edu.um.ingenieria.limitador.services.UserService;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/register should register user and return 201 Created with activated false")
    void shouldRegisterUserSuccessfully() throws Exception {
        UserRegisterDTO registerDto = UserRegisterDTO.builder()
            .username("newregistrant")
            .email("register@example.com")
            .password("secret123")
            .firstName("Grace")
            .lastName("Hopper")
            .address("Compiler Way 1")
            .phoneNumber("+1-555-GRACE")
            .build();

        UserDTO responseDto = UserDTO.builder()
            .id(42L)
            .username("newregistrant")
            .email("register@example.com")
            .activated(false)
            .firstName("Grace")
            .lastName("Hopper")
            .address("Compiler Way 1")
            .phoneNumber("+1-555-GRACE")
            .build();

        when(userService.register(any(UserRegisterDTO.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(42)))
            .andExpect(jsonPath("$.username", is("newregistrant")))
            .andExpect(jsonPath("$.email", is("register@example.com")))
            .andExpect(jsonPath("$.activated", is(false)))
            .andExpect(jsonPath("$.firstName", is("Grace")))
            .andExpect(jsonPath("$.lastName", is("Hopper")))
            .andExpect(jsonPath("$.address", is("Compiler Way 1")))
            .andExpect(jsonPath("$.phoneNumber", is("+1-555-GRACE")))
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 Bad Request when validation fails")
    void shouldReturnBadRequestWhenRegistrationDataIsInvalid() throws Exception {
        UserRegisterDTO invalidDto = UserRegisterDTO.builder()
            .username("")
            .email("not-an-email")
            .password("12")
            .firstName("")
            .phoneNumber("")
            .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 Bad Request when phoneNumber is missing")
    void shouldReturnBadRequestWhenPhoneNumberIsMissing() throws Exception {
        UserRegisterDTO dtoWithoutPhone = UserRegisterDTO.builder()
            .username("validuser")
            .email("valid@example.com")
            .password("secret123")
            .firstName("John")
            .lastName("Doe")
            .address("Some Address 123")
            .phoneNumber(null)
            .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoWithoutPhone)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register should return 409 Conflict with RFC 9457 ProblemDetail when username already exists")
    void shouldReturnConflictProblemDetailWhenUsernameAlreadyExists() throws Exception {
        UserRegisterDTO registerDto = UserRegisterDTO.builder()
            .username("existinguser")
            .email("new@example.com")
            .password("secret123")
            .firstName("Jane")
            .phoneNumber("+54 261 1111111")
            .build();

        when(userService.register(any(UserRegisterDTO.class)))
            .thenThrow(new UserAlreadyExistsException("Username 'existinguser' is already registered"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
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
            .andExpect(jsonPath("$.detail", is("Username 'existinguser' is already registered")));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 409 Conflict with RFC 9457 ProblemDetail when email already exists")
    void shouldReturnConflictProblemDetailWhenEmailAlreadyExists() throws Exception {
        UserRegisterDTO registerDto = UserRegisterDTO.builder()
            .username("newuser")
            .email("existing@example.com")
            .password("secret123")
            .firstName("Jane")
            .phoneNumber("+54 261 1111111")
            .build();

        when(userService.register(any(UserRegisterDTO.class)))
            .thenThrow(new UserAlreadyExistsException("Email 'existing@example.com' is already registered"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
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
            .andExpect(jsonPath("$.detail", is("Email 'existing@example.com' is already registered")));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 OK with token and success message when credentials are valid")
    void shouldReturnTokenAndSuccessMessageWhenLoginSucceeds() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("johndoe")
            .password("secret123")
            .rememberMe(false)
            .build();

        UserLoginResponseDTO responseDto = UserLoginResponseDTO.builder()
            .token("header.payload.signature")
            .message("Inicio de sesión exitoso")
            .build();

        when(userService.login(any(UserLoginDTO.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", is("header.payload.signature")))
            .andExpect(jsonPath("$.message", is("Inicio de sesión exitoso")));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 401 Unauthorized with RFC 9457 ProblemDetail on bad credentials")
    void shouldReturnUnauthorizedProblemDetailWhenLoginFails() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("johndoe")
            .password("wrongpassword")
            .build();

        when(userService.login(any(UserLoginDTO.class)))
            .thenThrow(new BadCredentialsException("Error de usuario y/o de password"));

        mockMvc.perform(post("/api/auth/login")
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
    @DisplayName("POST /api/auth/login should return 400 Bad Request when username or password is blank")
    void shouldReturnBadRequestWhenLoginFieldsAreBlank() throws Exception {
        UserLoginDTO invalidDto = UserLoginDTO.builder()
            .username("")
            .password("")
            .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
            .andExpect(status().isBadRequest());
    }
}
