package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttemptStatus;
import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.domain.BlacklistRuleType;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.BlacklistEvaluationResult;
import ar.edu.um.ingenieria.limitador.dto.BlacklistRuleDTO;
import ar.edu.um.ingenieria.limitador.exception.InvalidIpException;
import ar.edu.um.ingenieria.limitador.repository.BlacklistRuleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;
import ar.edu.um.ingenieria.limitador.validator.CidrMatcher;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlacklistService Specification")
class BlacklistServiceTest {

    @Mock
    private BlacklistRuleRepository blacklistRuleRepository;

    @Mock
    private UserRepository userRepository;

    private CidrMatcher cidrMatcher;
    private BlacklistServiceImpl blacklistService;

    @BeforeEach
    void setUp() {
        cidrMatcher = new CidrMatcher();
        blacklistService = new BlacklistServiceImpl(blacklistRuleRepository, userRepository, cidrMatcher);
    }

    @Test
    @DisplayName("Should block single IPv4 rule (192.168.1.50/32)")
    void shouldBlockSingleIpv4() {
        BlacklistRule ipRule = BlacklistRule.builder()
            .id(1L)
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("192.168.1.50/32")
            .reason("Brute force detected")
            .active(true)
            .build();

        when(blacklistRuleRepository.findByRuleTypeAndActiveTrue(BlacklistRuleType.IP_CIDR))
            .thenReturn(List.of(ipRule));

        assertThat(blacklistService.isIpBlocked("192.168.1.50")).isTrue();
        assertThat(blacklistService.isIpBlocked("192.168.1.51")).isFalse();
    }

    @Test
    @DisplayName("Should block IPv4 CIDR range (192.168.1.0/24)")
    void shouldBlockIpv4CidrRange() {
        BlacklistRule cidrRule = BlacklistRule.builder()
            .id(2L)
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("192.168.1.0/24")
            .reason("Blocked subnet")
            .active(true)
            .build();

        when(blacklistRuleRepository.findByRuleTypeAndActiveTrue(BlacklistRuleType.IP_CIDR))
            .thenReturn(List.of(cidrRule));

        assertThat(blacklistService.isIpBlocked("192.168.1.1")).isTrue();
        assertThat(blacklistService.isIpBlocked("192.168.1.250")).isTrue();
        assertThat(blacklistService.isIpBlocked("192.168.2.1")).isFalse();
    }

    @Test
    @DisplayName("Should block IPv6 CIDR range (2001:db8::/32)")
    void shouldBlockIpv6CidrRange() {
        BlacklistRule ipv6Rule = BlacklistRule.builder()
            .id(3L)
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("2001:db8::/32")
            .reason("Blocked IPv6 prefix")
            .active(true)
            .build();

        when(blacklistRuleRepository.findByRuleTypeAndActiveTrue(BlacklistRuleType.IP_CIDR))
            .thenReturn(List.of(ipv6Rule));

        assertThat(blacklistService.isIpBlocked("2001:db8::1")).isTrue();
        assertThat(blacklistService.isIpBlocked("2001:db8:abcd::99")).isTrue();
        assertThat(blacklistService.isIpBlocked("2001:db9::1")).isFalse();
    }

    @Test
    @DisplayName("Should evaluate simultaneous IP and User blocks accurately")
    void shouldEvaluateSimultaneousIpAndUserBlock() {
        User user = new User();
        user.setId(10L);
        user.setUsername("compromised_user");

        BlacklistRule ipRule = BlacklistRule.builder()
            .id(1L)
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("10.10.0.0/16")
            .reason("Botnet subnet")
            .active(true)
            .build();

        BlacklistRule userRule = BlacklistRule.builder()
            .id(2L)
            .ruleType(BlacklistRuleType.USER)
            .targetValue("compromised_user")
            .user(user)
            .reason("Account suspended")
            .active(true)
            .build();

        when(blacklistRuleRepository.findByRuleTypeAndActiveTrue(BlacklistRuleType.IP_CIDR))
            .thenReturn(List.of(ipRule));
        when(blacklistRuleRepository.findByRuleTypeAndTargetValueIgnoreCaseAndActiveTrue(
            BlacklistRuleType.USER, "compromised_user"))
            .thenReturn(List.of(userRule));
        when(blacklistRuleRepository.findByUser_IdAndActiveTrue(10L))
            .thenReturn(List.of(userRule));

        BlacklistEvaluationResult result = blacklistService.evaluate("10.10.5.25", "compromised_user", user);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.blockedByIp()).isTrue();
        assertThat(result.blockedByUser()).isTrue();
        assertThat(result.resolveAttemptStatus()).isEqualTo(AuthenticationAttemptStatus.BLOCKED_BY_IP_AND_USER);
        assertThat(result.buildRejectionReason()).contains("10.10.0.0/16", "compromised_user");
    }

    @Test
    @DisplayName("Should ignore expired blacklist rules")
    void shouldIgnoreExpiredBlacklistRules() {
        BlacklistRule expiredRule = BlacklistRule.builder()
            .id(4L)
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("192.168.1.50/32")
            .active(true)
            .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
            .build();

        when(blacklistRuleRepository.findByRuleTypeAndActiveTrue(BlacklistRuleType.IP_CIDR))
            .thenReturn(List.of(expiredRule));

        assertThat(blacklistService.isIpBlocked("192.168.1.50")).isFalse();
    }

    @Test
    @DisplayName("Should normalize single IP to /32 when creating IP_CIDR rule and reject invalid CIDR")
    void shouldNormalizeCidrOnCreateAndRejectInvalid() {
        BlacklistRuleDTO dto = BlacklistRuleDTO.builder()
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("192.168.1.50")
            .reason("Single host block")
            .build();

        when(blacklistRuleRepository.save(any(BlacklistRule.class))).thenAnswer(inv -> inv.getArgument(0));

        BlacklistRule saved = blacklistService.createRule(dto);
        assertThat(saved.getTargetValue()).isEqualTo("192.168.1.50/32");
        verify(blacklistRuleRepository).save(any(BlacklistRule.class));

        BlacklistRuleDTO invalidDto = BlacklistRuleDTO.builder()
            .ruleType(BlacklistRuleType.IP_CIDR)
            .targetValue("invalid.ip.range/99")
            .build();

        assertThatThrownBy(() -> blacklistService.createRule(invalidDto))
            .isInstanceOf(InvalidIpException.class);
    }
}
