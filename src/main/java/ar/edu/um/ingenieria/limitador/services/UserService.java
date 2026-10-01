package ar.edu.um.ingenieria.limitador.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.UserDTO;

public interface UserService {
    List<User> findAll();
    Page<User> findAll(Pageable pageable);
    Optional<User> findById(Long id);
    User save(User user);
    User update(Long id, User user);
    void deleteById(Long id);

    List<UserDTO> findAllDTOs();
    Page<UserDTO> findAllDTOs(Pageable pageable);
    Optional<UserDTO> findDTOById(Long id);
    UserDTO saveDTO(UserDTO userDTO);
    UserDTO updateDTO(Long id, UserDTO userDTO);
}
