package ar.edu.um.ingenieria.limitador.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import tools.jackson.databind.json.JsonMapper;

class UserLoginDTOTest {

    private static final String VALID_USERNAME = "johndoe";
    private static final String VALID_PASSWORD = "secret123";
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Should pass validation when username and password are provided")
    void shouldPassValidationWithValidCredentials() {
        UserLoginDTO dto = validLoginBuilder().build();

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    @DisplayName("Should default rememberMe to false when built without it")
    void shouldDefaultRememberMeToFalseWhenUsingBuilder() {
        UserLoginDTO dto = validLoginBuilder().build();

        assertThat(dto.isRememberMe()).isFalse();
    }

    @Test
    @DisplayName("Should default rememberMe to false when created with no-args constructor")
    void shouldDefaultRememberMeToFalseWhenUsingNoArgsConstructor() {
        UserLoginDTO dto = new UserLoginDTO();

        assertThat(dto.isRememberMe()).isFalse();
    }

    @ParameterizedTest(name = "payload = {0}")
    @ValueSource(strings = {
        "{\"username\": \"johndoe\", \"password\": \"secret123\"}",
        "{\"username\": \"johndoe\", \"password\": \"secret123\", \"rememberMe\": null}"
    })
    @DisplayName("Should default rememberMe to false when missing or null in the JSON payload")
    void shouldDefaultRememberMeToFalseWhenMissingOrNullInJson(String json) {
        UserLoginDTO dto = JSON_MAPPER.readValue(json, UserLoginDTO.class);

        assertThat(dto.isRememberMe()).isFalse();
    }

    @Test
    @DisplayName("Should keep rememberMe as true when explicitly requested")
    void shouldKeepRememberMeWhenExplicitlySetToTrue() {
        UserLoginDTO dto = validLoginBuilder().rememberMe(true).build();

        assertThat(dto.isRememberMe()).isTrue();
    }

    @ParameterizedTest(name = "username = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Should fail validation when username is null or blank")
    void shouldFailWhenUsernameIsNullOrBlank(String invalidUsername) {
        UserLoginDTO dto = validLoginBuilder().username(invalidUsername).build();

        assertSingleViolationOn(dto, "username");
    }

    @ParameterizedTest(name = "password = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Should fail validation when password is null or blank")
    void shouldFailWhenPasswordIsNullOrBlank(String invalidPassword) {
        UserLoginDTO dto = validLoginBuilder().password(invalidPassword).build();

        assertSingleViolationOn(dto, "password");
    }

    @Test
    @DisplayName("Should not expose the password in toString to avoid leaking it in logs")
    void shouldNotExposePasswordInToString() {
        UserLoginDTO dto = validLoginBuilder().build();

        assertThat(dto.toString()).doesNotContain(VALID_PASSWORD);
    }

    private UserLoginDTO.UserLoginDTOBuilder validLoginBuilder() {
        return UserLoginDTO.builder()
            .username(VALID_USERNAME)
            .password(VALID_PASSWORD);
    }

    private void assertSingleViolationOn(UserLoginDTO dto, String expectedField) {
        Set<ConstraintViolation<UserLoginDTO>> violations = validator.validate(dto);

        assertThat(violations)
            .singleElement()
            .satisfies(violation ->
                assertThat(violation.getPropertyPath()).hasToString(expectedField));
    }
}
