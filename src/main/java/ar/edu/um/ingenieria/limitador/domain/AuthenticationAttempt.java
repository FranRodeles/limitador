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
 * Entidad de auditoría que registra cada intento de autenticación (exitoso, fallido o bloqueado),
 * asociándolo con la dirección IP de origen y, cuando existe, con la cuenta de usuario correspondiente.
 */
@Entity
@Table(
    name = "authentication_attempts",
    indexes = {
        @Index(name = "idx_auth_attempts_user_id", columnList = "user_id"),
        @Index(name = "idx_auth_attempts_ip_id", columnList = "ip_address_id"),
        @Index(name = "idx_auth_attempts_attempted_at", columnList = "attempted_at"),
        @Index(name = "idx_auth_attempts_status", columnList = "status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user", "ipAddress"})
@ToString(exclude = {"user"})
public class AuthenticationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "attempted_username", nullable = false, length = 100)
    private String attemptedUsername;

    @Column(name = "user_identified", nullable = false)
    private Boolean userIdentified;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ip_address_id", nullable = false)
    private IpAddress ipAddress;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "successful", nullable = false)
    private Boolean successful;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AuthenticationAttemptStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
