package ar.edu.um.ingenieria.limitador.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttempt;
import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttemptStatus;
import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.domain.BlacklistRuleType;
import ar.edu.um.ingenieria.limitador.domain.IpAddress;
import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.BlacklistRuleDTO;
import ar.edu.um.ingenieria.limitador.dto.UserLoginDTO;
import ar.edu.um.ingenieria.limitador.repository.AuthenticationAttemptRepository;
import ar.edu.um.ingenieria.limitador.repository.IpAddressRepository;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;
import ar.edu.um.ingenieria.limitador.security.JwtService;
import ar.edu.um.ingenieria.limitador.services.BlacklistService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("IP Registration & Blacklist Integration Specification")
class IpRegistrationAndBlacklistIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private BlacklistService blacklistService;

    @Autowired
    private AuthenticationAttemptRepository authenticationAttemptRepository;

    @Autowired
    private IpAddressRepository ipAddressRepository;

    private static final String PASSWORD = "SecurePassword123!";
    private User alice;
    private User bob;
    private User admin;
    private String adminToken;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role();
        adminRole.setRoleName("ROLE_ADMIN");
        adminRole.setDescription("Administrator");
        adminRole = roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setRoleName("ROLE_USER");
        userRole.setDescription("Standard User");
        userRole = roleRepository.save(userRole);

        alice = new User();
        alice.setUsername("alice");
        alice.setEmail("alice@example.com");
        alice.setPassword(passwordEncoder.encode(PASSWORD));
        alice.setActivated(true);
        alice.setRoles(Set.of(userRole));
        alice = userRepository.save(alice);

        bob = new User();
        bob.setUsername("bob");
        bob.setEmail("bob@example.com");
        bob.setPassword(passwordEncoder.encode(PASSWORD));
        bob.setActivated(true);
        bob.setRoles(Set.of(userRole));
        bob = userRepository.save(bob);

        admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode(PASSWORD));
        admin.setActivated(true);
        admin.setRoles(Set.of(adminRole));
        admin = userRepository.save(admin);

        adminToken = jwtService.generateToken(admin, false);
    }

    @Test
    @DisplayName("1. Successful login records IP, geolocation (Country/City/ASN), user association, and timestamp")
    void shouldRecordIpAndGeolocationOnSuccessfulLogin() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("alice")
            .password(PASSWORD)
            .rememberMe(false)
            .build();

        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "8.8.8.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.message", is("Inicio de sesión exitoso")));

        List<AuthenticationAttempt> attempts = authenticationAttemptRepository.findAll();
        assertThat(attempts).hasSize(1);

        AuthenticationAttempt attempt = attempts.get(0);
        assertThat(attempt.getSuccessful()).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(AuthenticationAttemptStatus.SUCCESS);
        assertThat(attempt.getUserIdentified()).isTrue();
        assertThat(attempt.getUser().getUsername()).isEqualTo("alice");
        assertThat(attempt.getAttemptedAt()).isNotNull();

        IpAddress ipRecord = attempt.getIpAddress();
        assertThat(ipRecord.getIp()).isEqualTo("8.8.8.8");
        assertThat(ipRecord.getCountry()).isNotNull();
        assertThat(ipRecord.getAsn()).isNotNull();
    }

    @Test
    @DisplayName("2. Failed login (wrong password) records IP, associated user, failure status, and never generates JWT")
    void shouldRecordIpOnFailedLoginWithWrongPassword() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("alice")
            .password("WrongPass999!")
            .build();

        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "1.1.1.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.status", is(401)));

        List<AuthenticationAttempt> attempts = authenticationAttemptRepository.findAll();
        assertThat(attempts).hasSize(1);

        AuthenticationAttempt attempt = attempts.get(0);
        assertThat(attempt.getSuccessful()).isFalse();
        assertThat(attempt.getStatus()).isEqualTo(AuthenticationAttemptStatus.FAILURE_BAD_CREDENTIALS);
        assertThat(attempt.getUserIdentified()).isTrue();
        assertThat(attempt.getUser().getUsername()).isEqualTo("alice");
        assertThat(attempt.getIpAddress().getIp()).isEqualTo("1.1.1.1");
        assertThat(attempt.getRejectionReason()).isNotBlank();
    }

    @Test
    @DisplayName("3. Login attempt for non-existent user records IP with userIdentified=false and user=null")
    void shouldRecordAttemptForNonExistentUser() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("unknown_account")
            .password(PASSWORD)
            .build();

        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "8.8.4.4")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.token").doesNotExist());

        List<AuthenticationAttempt> attempts = authenticationAttemptRepository.findAll();
        assertThat(attempts).hasSize(1);

        AuthenticationAttempt attempt = attempts.get(0);
        assertThat(attempt.getSuccessful()).isFalse();
        assertThat(attempt.getUser()).isNull();
        assertThat(attempt.getUserIdentified()).isFalse();
        assertThat(attempt.getAttemptedUsername()).isEqualTo("unknown_account");
        assertThat(attempt.getStatus()).isEqualTo(AuthenticationAttemptStatus.FAILURE_USER_NOT_FOUND);
        assertThat(attempt.getIpAddress().getIp()).isEqualTo("8.8.4.4");
    }

    @Test
    @DisplayName("4. Trusted proxy vs untrusted remoteAddr: prevents X-Forwarded-For spoofing")
    void shouldExtractIpBehindTrustedProxyAndIgnoreUntrustedSpoofedHeader() throws Exception {
        UserLoginDTO loginDto = UserLoginDTO.builder()
            .username("alice")
            .password(PASSWORD)
            .build();

        // Untrusted remoteAddr (198.51.100.44) tries to spoof X-Forwarded-For: 8.8.8.8
        mockMvc.perform(post("/api/auth/login")
                .with(req -> {
                    req.setRemoteAddr("198.51.100.44");
                    return req;
                })
                .header("X-Forwarded-For", "8.8.8.8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
            .andExpect(status().isOk());

        List<AuthenticationAttempt> attempts = authenticationAttemptRepository.findAll();
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getIpAddress().getIp()).isEqualTo("198.51.100.44");
    }

    @Test
    @DisplayName("5. Blacklist single IP (192.168.1.50/32) blocks different users from that IP and records BLOCKED_BY_IP")
    void shouldBlockSingleIpAcrossMultipleUsersAndRecordAttempt() throws Exception {
        blacklistService.createRule(BlacklistRuleDTO.builder()
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("192.168.1.50/32")
            .reason("Malicious host")
            .build());

        // Alice tries from 192.168.1.50 -> blocked, no JWT
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "192.168.1.50")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("alice", PASSWORD, false))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.token").doesNotExist());

        // Bob tries from 192.168.1.50 -> also blocked, no JWT
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "192.168.1.50")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("bob", PASSWORD, false))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.token").doesNotExist());

        // Alice tries from 192.168.1.51 -> allowed!
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "192.168.1.51")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("alice", PASSWORD, false))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());

        List<AuthenticationAttempt> attempts = authenticationAttemptRepository.findByIpAddress_IpOrderByAttemptedAtDesc("192.168.1.50");
        assertThat(attempts).hasSize(2);
        assertThat(attempts).allSatisfy(a -> {
            assertThat(a.getSuccessful()).isFalse();
            assertThat(a.getStatus()).isEqualTo(AuthenticationAttemptStatus.BLOCKED_BY_IP);
            assertThat(a.getRejectionReason()).contains("192.168.1.50/32");
        });
    }

    @Test
    @DisplayName("6. Blacklist IPv4 CIDR range (192.168.1.0/24) and IPv6 CIDR range (2001:db8::/32)")
    void shouldBlockIpv4AndIpv6CidrRanges() throws Exception {
        blacklistService.createRule(BlacklistRuleDTO.builder()
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("192.168.1.0/24")
            .reason("Blocked IPv4 subnet")
            .build());

        blacklistService.createRule(BlacklistRuleDTO.builder()
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("2001:db8::/32")
            .reason("Blocked IPv6 prefix")
            .build());

        // IPv4 inside /24 -> blocked
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "192.168.1.199")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("alice", PASSWORD, false))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.token").doesNotExist());

        // IPv6 inside 2001:db8::/32 -> blocked
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "2001:db8:abcd:0001::1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("alice", PASSWORD, false))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.token").doesNotExist());

        // IPv6 outside 2001:db8::/32 -> allowed
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "2001:4860:4860::8888")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("alice", PASSWORD, false))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("7. Blocked user cannot login even when changing IP addresses, and records BLOCKED_BY_USER")
    void shouldBlockUserRegardlessOfIpAddress() throws Exception {
        blacklistService.createRule(BlacklistRuleDTO.builder()
            .ruleType(BlacklistRuleType.USER)
            .targetValue("alice")
            .reason("Compromised credentials")
            .build());

        // Alice from IP 1 -> blocked
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "203.0.113.10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("alice", PASSWORD, false))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.token").doesNotExist());

        // Alice changes IP to IP 2 -> still blocked
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "198.51.100.25")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("alice", PASSWORD, false))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.token").doesNotExist());

        // Bob from IP 1 -> allowed
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "203.0.113.10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("bob", PASSWORD, false))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());

        List<AuthenticationAttempt> aliceAttempts = authenticationAttemptRepository
            .findByAttemptedUsernameOrderByAttemptedAtDesc("alice");
        assertThat(aliceAttempts).hasSize(2);
        assertThat(aliceAttempts).allSatisfy(a -> {
            assertThat(a.getSuccessful()).isFalse();
            assertThat(a.getStatus()).isEqualTo(AuthenticationAttemptStatus.BLOCKED_BY_USER);
            assertThat(a.getRejectionReason()).contains("alice");
        });
    }

    @Test
    @DisplayName("8. Simultaneous User and IP block records BLOCKED_BY_IP_AND_USER")
    void shouldRecordSimultaneousUserAndIpBlock() throws Exception {
        blacklistService.createRule(BlacklistRuleDTO.builder()
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("192.168.1.0/24")
            .reason("Blocked LAN")
            .build());

        blacklistService.createRule(BlacklistRuleDTO.builder()
            .ruleType(BlacklistRuleType.USER)
            .targetValue("alice")
            .reason("Blocked account")
            .build());

        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "192.168.1.50")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("alice", PASSWORD, false))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.token").doesNotExist());

        List<AuthenticationAttempt> attempts = authenticationAttemptRepository.findAll();
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getStatus()).isEqualTo(AuthenticationAttemptStatus.BLOCKED_BY_IP_AND_USER);
        assertThat(attempts.get(0).getRejectionReason()).contains("192.168.1.0/24", "alice");
    }

    @Test
    @DisplayName("9. Updating or deleting a blacklist rule via REST API immediately updates access control")
    void shouldAllowLoginAfterUpdatingOrDeletingBlacklistRule() throws Exception {
        // Admin creates IP block via REST controller
        BlacklistRuleDTO createDto = BlacklistRuleDTO.builder()
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("203.0.113.50/32")
            .reason("Temporary block")
            .active(true)
            .build();

        String createdJson = mockMvc.perform(post("/api/blacklist")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.targetValue", is("203.0.113.50/32")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        BlacklistRuleDTO createdRule = objectMapper.readValue(createdJson, BlacklistRuleDTO.class);

        // Login from 203.0.113.50 is blocked
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "203.0.113.50")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("alice", PASSWORD, false))))
            .andExpect(status().isForbidden());

        // Admin deactivates the rule via PUT /api/blacklist/{id}
        createdRule.setActive(false);
        mockMvc.perform(put("/api/blacklist/" + createdRule.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createdRule)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active", is(false)));

        // Login from 203.0.113.50 now succeeds
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "203.0.113.50")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserLoginDTO("alice", PASSWORD, false))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());

        // Admin deletes rule via DELETE /api/blacklist/{id}
        mockMvc.perform(delete("/api/blacklist/" + createdRule.getId())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        // Admin queries audit attempts via GET /api/auth-attempts
        mockMvc.perform(get("/api/auth-attempts")
                .param("username", "alice")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }
}
