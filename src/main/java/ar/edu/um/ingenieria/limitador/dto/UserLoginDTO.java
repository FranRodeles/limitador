package ar.edu.um.ingenieria.limitador.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
public class UserLoginDTO implements Serializable {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @ToString.Exclude
    private String password;

    private boolean rememberMe;

    /**
     * Single entry point used by both the builder and Jackson.
     * A missing or {@code null} rememberMe is normalized to {@code false}.
     */
    @Builder
    public UserLoginDTO(String username, String password, Boolean rememberMe) {
        this.username = username;
        this.password = password;
        this.rememberMe = Boolean.TRUE.equals(rememberMe);
    }
}
