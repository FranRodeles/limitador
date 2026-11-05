package ar.edu.um.ingenieria.limitador.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Entidad que representa una regla de bloqueo en la blacklist, ya sea por dirección IP/rango CIDR
 * ({@link BlacklistRuleType#IP_CIDR}) o por cuenta de usuario ({@link BlacklistRuleType#USER}).
 * Soporta motivo del bloqueo y fecha opcional de expiración para bloqueos temporales.
 */
@Entity
@Table(
    name = "blacklist_rules",
    indexes = {
        @Index(name = "idx_blacklist_type_active", columnList = "rule_type, active"),
        @Index(name = "idx_blacklist_target_type", columnList = "target_value, rule_type"),
        @Index(name = "idx_blacklist_user_id", columnList = "user_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user"})
@ToString(exclude = {"user"})
public class BlacklistRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private BlacklistRuleType ruleType;

    @Column(name = "target_value", nullable = false, length = 100)
    private String targetValue;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "reason", length = 500)
    private String reason;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public boolean isCurrentlyActive(Instant referenceTime) {
        if (!Boolean.TRUE.equals(active)) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(referenceTime);
    }
}
