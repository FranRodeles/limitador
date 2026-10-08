package ar.edu.um.ingenieria.limitador.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UserRegisterDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Should pass validation when all required fields are valid")
    void shouldPassValidationWithValidFields() {
        UserRegisterDTO dto = UserRegisterDTO.builder()
            .username("johndoe")
            .email("john.doe@example.com")
            .password("secret123")
            .firstName("John")
            .lastName("Doe")
            .address("Baker Street 221B")
            .phoneNumber("+54 261 1234567")
            .build();

        Set<ConstraintViolation<UserRegisterDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when phone number is blank or null")
    void shouldFailWhenPhoneNumberIsBlankOrNull() {
        UserRegisterDTO dto = UserRegisterDTO.builder()
            .username("johndoe")
            .email("john.doe@example.com")
            .password("secret123")
            .firstName("John")
            .lastName("Doe")
            .address("Baker Street 221B")
            .phoneNumber("")
            .build();

        Set<ConstraintViolation<UserRegisterDTO>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phoneNumber"));
    }

    @Test
    @DisplayName("Should fail validation when username is blank or too short")
    void shouldFailWhenUsernameIsBlankOrTooShort() {
        UserRegisterDTO dto = UserRegisterDTO.builder()
            .username(" ")
            .email("john.doe@example.com")
            .password("secret123")
            .firstName("John")
            .lastName("Doe")
            .address("Baker Street 221B")
            .build();

        Set<ConstraintViolation<UserRegisterDTO>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("Should fail validation when email is invalid")
    void shouldFailWhenEmailIsInvalid() {
        UserRegisterDTO dto = UserRegisterDTO.builder()
            .username("johndoe")
            .email("invalid-email-format")
            .password("secret123")
            .firstName("John")
            .lastName("Doe")
            .address("Baker Street 221B")
            .build();

        Set<ConstraintViolation<UserRegisterDTO>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Should fail validation when password is too short")
    void shouldFailWhenPasswordIsTooShort() {
        UserRegisterDTO dto = UserRegisterDTO.builder()
            .username("johndoe")
            .email("john.doe@example.com")
            .password("123")
            .firstName("John")
            .lastName("Doe")
            .address("Baker Street 221B")
            .build();

        Set<ConstraintViolation<UserRegisterDTO>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Should fail validation when first name is blank")
    void shouldFailWhenFirstNameIsBlank() {
        UserRegisterDTO dto = UserRegisterDTO.builder()
            .username("johndoe")
            .email("john.doe@example.com")
            .password("secret123")
            .firstName("")
            .lastName("Doe")
            .address("Baker Street 221B")
            .build();

        Set<ConstraintViolation<UserRegisterDTO>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("firstName"));
    }
}
