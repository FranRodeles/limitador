package ar.edu.um.ingenieria.limitador.services;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttemptStatus;
import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.BlacklistEvaluationResult;
import ar.edu.um.ingenieria.limitador.dto.UserDTO;
import ar.edu.um.ingenieria.limitador.dto.UserLoginDTO;
import ar.edu.um.ingenieria.limitador.dto.UserLoginResponseDTO;
import ar.edu.um.ingenieria.limitador.dto.UserRegisterDTO;
import ar.edu.um.ingenieria.limitador.exception.BlacklistedAccessException;
import ar.edu.um.ingenieria.limitador.exception.UserAlreadyExistsException;
import ar.edu.um.ingenieria.limitador.mapper.UserMapper;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;
import ar.edu.um.ingenieria.limitador.security.ClientIpResolver;
import ar.edu.um.ingenieria.limitador.security.JwtService;

@Service
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_LOCAL_IP = "127.0.0.1";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final BlacklistService blacklistService;
    private final AuthenticationAttemptService authenticationAttemptService;
    private final ClientIpResolver clientIpResolver;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           UserMapper userMapper, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtService jwtService) {
        this(userRepository, roleRepository, userMapper, passwordEncoder,
             authenticationManager, jwtService, null, null, null);
    }

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           UserMapper userMapper, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtService jwtService,
                           BlacklistService blacklistService,
                           AuthenticationAttemptService authenticationAttemptService,
                           ClientIpResolver clientIpResolver) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.authenticationAttemptService = authenticationAttemptService;
        this.clientIpResolver = clientIpResolver;
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
        if (entity.getActivated() == null) {
            entity.setActivated(false);
        }
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
        if (userDTO.getPassword() == null || userDTO.getPassword().isBlank()) {
            entity.setPassword(existing.getPassword());
        } else {
            entity.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }
        if (entity.getActivated() == null) {
            entity.setActivated(existing.getActivated());
        }
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
        entity.setRoles(new HashSet<>(Set.of(findOrCreateRole("ROLE_USER"))));

        if (entity.getUserData() != null) {
            entity.getUserData().setId(null);
            entity.getUserData().setUser(entity);
        }

        User saved;
        try {
            saved = userRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            // A concurrent registration can slip past the exists* checks: the unique
            // constraints on username/email are the source of truth, so translate the
            // violation into the same domain error the sequential path produces.
            throw translateDuplicateRegistration(registerDTO, ex);
        }
        return userMapper.toDto(saved).withoutPassword();
    }

    private UserAlreadyExistsException translateDuplicateRegistration(
            UserRegisterDTO registerDTO, DataIntegrityViolationException violation) {
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            return new UserAlreadyExistsException(
                "Username '" + registerDTO.getUsername() + "' is already registered");
        }
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            return new UserAlreadyExistsException(
                "Email '" + registerDTO.getEmail() + "' is already registered");
        }
        throw violation;
    }

    @Override
    public UserLoginResponseDTO login(UserLoginDTO loginDTO) {
        String resolvedIp = clientIpResolver != null
            ? clientIpResolver.resolveCurrentClientIp()
            : DEFAULT_LOCAL_IP;
        return login(loginDTO, resolvedIp);
    }

    @Override
    public UserLoginResponseDTO login(UserLoginDTO loginDTO, String clientIp) {
        if (loginDTO == null) {
            throw new IllegalArgumentException("Login data cannot be null");
        }

        String resolvedIp = (clientIp != null && !clientIp.isBlank())
            ? clientIp.trim()
            : (clientIpResolver != null ? clientIpResolver.resolveCurrentClientIp() : DEFAULT_LOCAL_IP);

        String username = loginDTO.getUsername() != null ? loginDTO.getUsername().trim() : "";
        User identifiedUser = (userRepository != null && !username.isEmpty())
            ? userRepository.findByUsername(username).orElse(null)
            : null;

        // Consultar la blacklist por IP/CIDR y por usuario antes de autenticar o emitir JWT
        if (blacklistService != null) {
            BlacklistEvaluationResult evaluation = blacklistService.evaluate(resolvedIp, username, identifiedUser);
            if (evaluation != null && evaluation.isBlocked()) {
                recordAttemptSafe(
                    resolvedIp,
                    username,
                    identifiedUser,
                    false,
                    evaluation.resolveAttemptStatus(),
                    evaluation.buildRejectionReason()
                );
                throw new BlacklistedAccessException("Acceso bloqueado por políticas de seguridad");
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            if (identifiedUser == null && userDetails instanceof User principalUser) {
                identifiedUser = principalUser;
            }

            String token = jwtService.generateToken(userDetails, loginDTO.isRememberMe());

            recordAttemptSafe(
                resolvedIp,
                username,
                identifiedUser,
                true,
                AuthenticationAttemptStatus.SUCCESS,
                null
            );

            return UserLoginResponseDTO.builder()
                .token(token)
                .message("Inicio de sesión exitoso")
                .build();
        } catch (AuthenticationException ex) {
            AuthenticationAttemptStatus failureStatus;
            String failureReason;

            if (identifiedUser == null) {
                failureStatus = AuthenticationAttemptStatus.FAILURE_USER_NOT_FOUND;
                failureReason = "El usuario no existe; intento no asociado a una cuenta registrada";
            } else if (ex instanceof DisabledException || Boolean.FALSE.equals(identifiedUser.getActivated())) {
                failureStatus = AuthenticationAttemptStatus.FAILURE_USER_INACTIVE;
                failureReason = "Cuenta de usuario inactiva";
            } else {
                failureStatus = AuthenticationAttemptStatus.FAILURE_BAD_CREDENTIALS;
                failureReason = "Credenciales inválidas";
            }

            recordAttemptSafe(
                resolvedIp,
                username,
                identifiedUser,
                false,
                failureStatus,
                failureReason
            );

            throw new BadCredentialsException("Error de usuario y/o de password", ex);
        }
    }

    private void recordAttemptSafe(
            String clientIp,
            String attemptedUsername,
            User identifiedUser,
            boolean successful,
            AuthenticationAttemptStatus status,
            String rejectionReason) {
        if (authenticationAttemptService != null) {
            authenticationAttemptService.recordAttempt(
                clientIp,
                attemptedUsername,
                identifiedUser,
                successful,
                status,
                rejectionReason
            );
        }
    }

    private Set<Role> resolveRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty() || roleRepository == null) {
            return roles;
        }
        Set<Role> resolved = new HashSet<>();
        for (Role role : roles) {
            if (role.getRoleName() != null) {
                resolved.add(findOrCreateRole(role));
            }
        }
        return resolved;
    }

    private Role findOrCreateRole(String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        role.setDescription(roleName);
        return findOrCreateRole(role);
    }

    private Role findOrCreateRole(Role role) {
        return roleRepository.findByRoleName(role.getRoleName())
            .orElseGet(() -> {
                try {
                    return roleRepository.save(role);
                } catch (DataIntegrityViolationException ex) {
                    // A concurrent request created the same role between our lookup and
                    // the insert: the winner's row is now committed, so reuse it.
                    return roleRepository.findByRoleName(role.getRoleName())
                        .orElseThrow(() -> ex);
                }
            });
    }
}
