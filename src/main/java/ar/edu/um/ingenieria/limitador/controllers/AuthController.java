package ar.edu.um.ingenieria.limitador.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import ar.edu.um.ingenieria.limitador.dto.UserDTO;
import ar.edu.um.ingenieria.limitador.dto.UserLoginDTO;
import ar.edu.um.ingenieria.limitador.dto.UserLoginResponseDTO;
import ar.edu.um.ingenieria.limitador.dto.UserRegisterDTO;
import ar.edu.um.ingenieria.limitador.services.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        UserDTO registered = userService.register(registerDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        UserLoginResponseDTO response = userService.login(loginDTO);
        return ResponseEntity.ok(response);
    }
}
