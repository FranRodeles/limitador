package ar.edu.um.ingenieria.limitador.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttempt;
import ar.edu.um.ingenieria.limitador.domain.IpAddress;
import ar.edu.um.ingenieria.limitador.dto.AuthenticationAttemptDTO;
import ar.edu.um.ingenieria.limitador.services.AuthenticationAttemptService;

@RestController
@RequestMapping("/api/auth-attempts")
@PreAuthorize("hasRole('ADMIN')")
public class AuthenticationAttemptController {

    private final AuthenticationAttemptService authenticationAttemptService;

    public AuthenticationAttemptController(AuthenticationAttemptService authenticationAttemptService) {
        this.authenticationAttemptService = authenticationAttemptService;
    }

    @GetMapping
    public ResponseEntity<List<AuthenticationAttemptDTO>> findAttempts(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "ip", required = false) String ip) {
        List<AuthenticationAttempt> attempts;
        if (username != null && !username.isBlank()) {
            attempts = authenticationAttemptService.findByUsername(username);
        } else if (ip != null && !ip.isBlank()) {
            attempts = authenticationAttemptService.findByIp(ip);
        } else {
            attempts = authenticationAttemptService.findAll();
        }

        List<AuthenticationAttemptDTO> dtos = attempts.stream()
            .map(authenticationAttemptService::toDTO)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/ips")
    public ResponseEntity<List<IpAddress>> findAllRegisteredIps() {
        return ResponseEntity.ok(authenticationAttemptService.findAllRegisteredIps());
    }
}
