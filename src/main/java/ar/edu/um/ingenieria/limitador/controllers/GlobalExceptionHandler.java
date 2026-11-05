package ar.edu.um.ingenieria.limitador.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ar.edu.um.ingenieria.limitador.exception.BlacklistedAccessException;
import ar.edu.um.ingenieria.limitador.exception.InvalidIpException;
import ar.edu.um.ingenieria.limitador.exception.IpNotFoundException;
import ar.edu.um.ingenieria.limitador.exception.PrivateIpAddressException;
import ar.edu.um.ingenieria.limitador.exception.UserAlreadyExistsException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidIpException.class)
    public ProblemDetail handleInvalidIp(InvalidIpException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Invalid IP Address");
        return problemDetail;
    }

    @ExceptionHandler(PrivateIpAddressException.class)
    public ProblemDetail handlePrivateIp(PrivateIpAddressException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Private IP Address");
        return problemDetail;
    }

    @ExceptionHandler(IpNotFoundException.class)
    public ProblemDetail handleIpNotFound(IpNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("IP Address Not Found");
        return problemDetail;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("User Already Exists");
        return problemDetail;
    }

    @ExceptionHandler(BlacklistedAccessException.class)
    public ProblemDetail handleBlacklistedAccess(BlacklistedAccessException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Acceso bloqueado por políticas de seguridad"
        );
        problemDetail.setTitle("Forbidden");
        return problemDetail;
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Error de usuario y/o de password"
        );
        problemDetail.setTitle("Unauthorized");
        return problemDetail;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleAccessDenied() {
        return ar.edu.um.ingenieria.limitador.security.UnauthorizedAccessHandler.problemDetail();
    }
}
