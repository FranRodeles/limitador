package ar.edu.um.ingenieria.limitador.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.domain.BlacklistRuleType;
import ar.edu.um.ingenieria.limitador.dto.BlacklistRuleDTO;
import ar.edu.um.ingenieria.limitador.services.BlacklistService;

@RestController
@RequestMapping("/api/blacklist")
@PreAuthorize("hasRole('ADMIN')")
public class BlacklistController {

    private final BlacklistService blacklistService;

    public BlacklistController(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @GetMapping
    public ResponseEntity<List<BlacklistRuleDTO>> findAll(
            @RequestParam(value = "type", required = false) BlacklistRuleType type) {
        List<BlacklistRule> rules = (type != null)
            ? blacklistService.findByType(type)
            : blacklistService.findAll();
        List<BlacklistRuleDTO> dtos = rules.stream()
            .map(blacklistService::toDTO)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlacklistRuleDTO> findById(@PathVariable("id") Long id) {
        return blacklistService.findById(id)
            .map(blacklistService::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BlacklistRuleDTO> create(@Valid @RequestBody BlacklistRuleDTO ruleDTO) {
        BlacklistRule created = blacklistService.createRule(ruleDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(blacklistService.toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlacklistRuleDTO> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody BlacklistRuleDTO ruleDTO) {
        BlacklistRule updated = blacklistService.updateRule(id, ruleDTO);
        return ResponseEntity.ok(blacklistService.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id) {
        blacklistService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
