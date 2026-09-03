package ar.edu.um.ingenieria.limitador.services;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.UserDTO;
import ar.edu.um.ingenieria.limitador.mapper.UserMapper;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public User update(Long id, User user) {
        if (!userRepository.findById(id).isPresent()) {
            throw new RuntimeException("User not found with id: " + id);
        }
        user.setId(id);
        return userRepository.save(user);
    }

    @Override
    public void deleteById(Long id) {
        if (!userRepository.findById(id).isPresent()) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public List<UserDTO> findAllDTOs() {
        return userMapper.toDtoList(userRepository.findAll());
    }

    @Override
    public Optional<UserDTO> findDTOById(Long id) {
        return userRepository.findById(id).map(userMapper::toDto);
    }

    @Override
    public UserDTO saveDTO(UserDTO userDTO) {
        User entity = userMapper.toEntity(userDTO);
        if (entity.getRoles() != null && !entity.getRoles().isEmpty()) {
            entity.setRoles(resolveRoles(entity.getRoles()));
        }
        User saved = userRepository.save(entity);
        return userMapper.toDto(saved);
    }

    @Override
    public UserDTO updateDTO(Long id, UserDTO userDTO) {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        User entity = userMapper.toEntity(userDTO);
        entity.setId(id);
        if (existing.getUserData() != null && entity.getUserData() != null) {
            entity.getUserData().setId(existing.getUserData().getId());
        }
        if (entity.getRoles() != null && !entity.getRoles().isEmpty()) {
            entity.setRoles(resolveRoles(entity.getRoles()));
        }
        User updated = userRepository.save(entity);
        return userMapper.toDto(updated);
    }

    private Set<Role> resolveRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty() || roleRepository == null) {
            return roles;
        }
        Set<Role> resolved = new HashSet<>();
        for (Role role : roles) {
            if (role.getRoleName() != null) {
                Role existingRole = roleRepository.findByRoleName(role.getRoleName())
                    .orElseGet(() -> roleRepository.save(role));
                resolved.add(existingRole);
            }
        }
        return resolved;
    }
}
