package ar.edu.um.ingenieria.limitador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttempt;
import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttemptStatus;

public interface AuthenticationAttemptRepository extends JpaRepository<AuthenticationAttempt, Long> {
    List<AuthenticationAttempt> findByUserIdOrderByAttemptedAtDesc(Long userId);
    List<AuthenticationAttempt> findByAttemptedUsernameOrderByAttemptedAtDesc(String attemptedUsername);
    List<AuthenticationAttempt> findByIpAddress_IpOrderByAttemptedAtDesc(String ip);
    List<AuthenticationAttempt> findByStatusOrderByAttemptedAtDesc(AuthenticationAttemptStatus status);
}
