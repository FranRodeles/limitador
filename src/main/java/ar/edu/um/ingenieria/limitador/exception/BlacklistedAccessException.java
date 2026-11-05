package ar.edu.um.ingenieria.limitador.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Excepción lanzada cuando un intento de autenticación es rechazado por coincidir
 * con una regla activa de la blacklist (por IP/CIDR, por usuario o ambos).
 */
public class BlacklistedAccessException extends AuthenticationException {

    public BlacklistedAccessException(String msg) {
        super(msg);
    }

    public BlacklistedAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
