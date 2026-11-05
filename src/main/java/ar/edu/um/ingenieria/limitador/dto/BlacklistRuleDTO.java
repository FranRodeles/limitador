package ar.edu.um.ingenieria.limitador.dto;

import java.io.Serializable;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRuleType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistRuleDTO implements Serializable {

    private Long id;

    @NotNull(message = "Rule type is required")
    private BlacklistRuleType ruleType;

    @NotBlank(message = "Target value is required")
    private String targetValue;

    private Long userId;

    private String reason;

    @Builder.Default
    private Boolean active = true;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant expiresAt;
}
