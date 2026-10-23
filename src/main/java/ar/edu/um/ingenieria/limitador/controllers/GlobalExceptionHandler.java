package ar.edu.um.ingenieria.limitador.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ar.edu.um.ingenieria.limitador.exception.UserAlreadyExistsException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("User Already Exists");
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
