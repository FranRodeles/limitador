package ar.edu.um.ingenieria.limitador.controllers;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import ar.edu.um.ingenieria.limitador.dto.UserDTO;
import ar.edu.um.ingenieria.limitador.dto.UserRegisterDTO;
import ar.edu.um.ingenieria.limitador.services.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<UserDTO>> findAll(
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.findAllDTOs(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<UserDTO> searchByUsernameOrEmail(
            @RequestParam("username") String username,
            @RequestParam("email") String email) {
        if (username.isBlank() || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return userService.findDTOByUsernameOrEmail(username, email)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> findById(@PathVariable("id") Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal ar.edu.um.ingenieria.limitador.domain.User principal) {
        authorizeProfileAccess(principal, id);
        return userService.findDTOById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserDTO> save(@RequestBody UserDTO userDTO) {
        UserDTO saved = userService.saveDTO(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        UserDTO registered = userService.register(registerDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }

    @PostMapping("/login")
    public ResponseEntity<ar.edu.um.ingenieria.limitador.dto.UserLoginResponseDTO> login(
            @Valid @RequestBody ar.edu.um.ingenieria.limitador.dto.UserLoginDTO loginDTO) {
        ar.edu.um.ingenieria.limitador.dto.UserLoginResponseDTO response = userService.login(loginDTO);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable("id") Long id, @RequestBody UserDTO userDTO,
            @org.springframework.security.core.annotation.AuthenticationPrincipal ar.edu.um.ingenieria.limitador.domain.User principal) {
        authorizeProfileAccess(principal, id);
        UserDTO updated = userService.updateDTO(id, userDTO);
        return ResponseEntity.ok(updated);
    }

    private void authorizeProfileAccess(ar.edu.um.ingenieria.limitador.domain.User principal, Long id) {
        if (principal == null) {
            return;
        }
        boolean isAdmin = principal.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = principal.getId() != null && principal.getId().equals(id);
        if (!isAdmin && !isOwner) {
            throw new org.springframework.security.access.AccessDeniedException("No autorizado");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
