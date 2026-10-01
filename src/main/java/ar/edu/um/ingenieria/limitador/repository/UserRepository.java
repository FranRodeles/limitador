package ar.edu.um.ingenieria.limitador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.ingenieria.limitador.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndEmail(String username, String email);
}
