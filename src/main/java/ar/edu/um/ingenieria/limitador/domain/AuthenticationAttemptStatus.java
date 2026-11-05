package ar.edu.um.ingenieria.limitador.domain;

/**
 * Resultado detallado de un intento de autenticación para auditoría.
 */
public enum AuthenticationAttemptStatus {
    SUCCESS,
    FAILURE_BAD_CREDENTIALS,
    FAILURE_USER_NOT_FOUND,
    FAILURE_USER_INACTIVE,
    BLOCKED_BY_IP,
    BLOCKED_BY_USER,
    BLOCKED_BY_IP_AND_USER
}
