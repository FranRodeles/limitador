package ar.edu.um.ingenieria.limitador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.ingenieria.limitador.domain.IpAddress;

public interface IpAddressRepository extends JpaRepository<IpAddress, Long> {
    Optional<IpAddress> findByIp(String ip);
    boolean existsByIp(String ip);
}
