package ar.edu.um.ingenieria.limitador.services;

import java.util.List;

import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttempt;
import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttemptStatus;
import ar.edu.um.ingenieria.limitador.domain.IpAddress;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.AuthenticationAttemptDTO;

public interface AuthenticationAttemptService {

    IpAddress resolveOrCreateIpAddress(String rawClientIp);

    AuthenticationAttempt recordAttempt(
        String clientIp,
        String attemptedUsername,
        User identifiedUser,
        boolean successful,
        AuthenticationAttemptStatus status,
        String rejectionReason
    );

    List<AuthenticationAttempt> findAll();

    List<AuthenticationAttempt> findByUsername(String username);

    List<AuthenticationAttempt> findByIp(String ip);

    List<IpAddress> findAllRegisteredIps();

    AuthenticationAttemptDTO toDTO(AuthenticationAttempt attempt);
}
