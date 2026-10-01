package ar.edu.um.ingenieria.limitador.dto;

import java.util.Set;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO implements Serializable {
    private Long id;
    private String username;
    private String email;
    private String password;
    private Boolean activated;
    private Set<String> roles;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;

    public UserDTO withoutPassword() {
        this.password = null;
        return this;
    }
}
