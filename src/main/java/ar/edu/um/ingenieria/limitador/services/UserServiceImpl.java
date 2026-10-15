package ar.edu.um.ingenieria.limitador.services;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.UserDTO;
import ar.edu.um.ingenieria.limitador.dto.UserLoginDTO;
import ar.edu.um.ingenieria.limitador.dto.UserLoginResponseDTO;
import ar.edu.um.ingenieria.limitador.dto.UserRegisterDTO;
import ar.edu.um.ingenieria.limitador.exception.UserAlreadyExistsException;
import ar.edu.um.ingenieria.limitador.mapper.UserMapper;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;
import ar.edu.um.ingenieria.limitador.security.JwtService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, 
                          UserMapper userMapper, PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User save(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User update(Long id, User user) {
        if (!userRepository.findById(id).isPresent()) {
            throw new RuntimeException("User not found with id: " + id);
        }
        user.setId(id);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
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
        return userMapper.toDtoList(userRepository.findAll()).stream()
            .map(UserDTO::withoutPassword)
            .toList();
    }

    @Override
    public Page<UserDTO> findAllDTOs(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(userMapper::toDto)
            .map(UserDTO::withoutPassword);
    }

    @Override
    public Optional<UserDTO> findDTOById(Long id) {
        return userRepository.findById(id)
            .map(userMapper::toDto)
            .map(UserDTO::withoutPassword);
    }

    @Override
    public Optional<UserDTO> findDTOByUsernameOrEmail(String username, String email) {
        if (username == null || username.isBlank() || email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsernameOrEmail(username.trim(), email.trim())
            .map(userMapper::toDto)
            .map(UserDTO::withoutPassword);
    }

    @Override
    public UserDTO saveDTO(UserDTO userDTO) {
        User entity = userMapper.toEntity(userDTO);
        entity.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        if (entity.getUserData() != null) {
            entity.getUserData().setId(null);
        }
        if (entity.getRoles() != null && !entity.getRoles().isEmpty()) {
            entity.getRoles().forEach(role -> role.setId(null));
            entity.setRoles(resolveRoles(entity.getRoles()));
        }
        User saved = userRepository.save(entity);
        return userMapper.toDto(saved).withoutPassword();
    }

    @Override
    public UserDTO updateDTO(Long id, UserDTO userDTO) {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        User entity = userMapper.toEntity(userDTO);
        entity.setId(id);
        entity.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        if (existing.getUserData() != null && entity.getUserData() != null) {
            entity.getUserData().setId(existing.getUserData().getId());
        }
        if (entity.getRoles() != null && !entity.getRoles().isEmpty()) {
            entity.setRoles(resolveRoles(entity.getRoles()));
        }
        User updated = userRepository.save(entity);
        return userMapper.toDto(updated).withoutPassword();
    }

    @Override
    public UserDTO register(UserRegisterDTO registerDTO) {
        if (registerDTO == null) {
            throw new IllegalArgumentException("Registration data cannot be null");
        }

        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + registerDTO.getUsername() + "' is already registered");
        }

        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + registerDTO.getEmail() + "' is already registered");
        }

        User entity = userMapper.toEntity(registerDTO);
        entity.setActivated(false);
        entity.setPassword(passwordEncoder.encode(registerDTO.getPassword()));

        if (entity.getUserData() != null) {
            entity.getUserData().setId(null);
            entity.getUserData().setUser(entity);
        }

        User saved = userRepository.save(entity);
        return userMapper.toDto(saved).withoutPassword();
    }

    @Override
    public UserLoginResponseDTO login(UserLoginDTO loginDTO) {
        if (loginDTO == null) {
            throw new IllegalArgumentException("Login data cannot be null");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );

            UserDetails user = (UserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(user, loginDTO.isRememberMe());

            return UserLoginResponseDTO.builder()
                .token(token)
                .message("Inicio de sesión exitoso")
                .build();
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Error de usuario y/o de password", ex);
        }
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
