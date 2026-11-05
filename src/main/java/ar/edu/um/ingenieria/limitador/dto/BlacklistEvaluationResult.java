package ar.edu.um.ingenieria.limitador.dto;

import java.util.List;
import java.util.stream.Collectors;

import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttemptStatus;
import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;

/**
 * Resultado inmutable de evaluar simultáneamente las reglas de blacklist por IP/CIDR y por usuario.
 */
public record BlacklistEvaluationResult(
    boolean blockedByIp,
    boolean blockedByUser,
    List<BlacklistRule> matchedIpRules,
    List<BlacklistRule> matchedUserRules
) {
    public boolean isBlocked() {
        return blockedByIp || blockedByUser;
    }

    public AuthenticationAttemptStatus resolveAttemptStatus() {
        if (blockedByIp && blockedByUser) {
            return AuthenticationAttemptStatus.BLOCKED_BY_IP_AND_USER;
        }
        if (blockedByIp) {
            return AuthenticationAttemptStatus.BLOCKED_BY_IP;
        }
        if (blockedByUser) {
            return AuthenticationAttemptStatus.BLOCKED_BY_USER;
        }
        return null;
    }

    public String buildRejectionReason() {
        if (!isBlocked()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (blockedByIp) {
            String ipRulesSummary = matchedIpRules.stream()
                .map(r -> r.getTargetValue() + (r.getReason() != null ? " (" + r.getReason() + ")" : ""))
                .collect(Collectors.joining(", "));
            sb.append("IP bloqueada por regla(s): ").append(ipRulesSummary);
        }
        if (blockedByUser) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            String userRulesSummary = matchedUserRules.stream()
                .map(r -> r.getTargetValue() + (r.getReason() != null ? " (" + r.getReason() + ")" : ""))
                .collect(Collectors.joining(", "));
            sb.append("Usuario bloqueado por regla(s): ").append(userRulesSummary);
        }
        return sb.toString();
    }
}
