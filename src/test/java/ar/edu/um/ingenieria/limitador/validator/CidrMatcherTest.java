package ar.edu.um.ingenieria.limitador.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.edu.um.ingenieria.limitador.exception.InvalidIpException;

@DisplayName("CidrMatcher Specification (Apache Commons Net SubnetUtils + IPv6 CIDR)")
class CidrMatcherTest {

    private CidrMatcher cidrMatcher;

    @BeforeEach
    void setUp() {
        cidrMatcher = new CidrMatcher();
    }

    @Test
    @DisplayName("Should match exact IPv4 with /32 CIDR rule using SubnetUtils")
    void shouldMatchExactIpv4WithSlash32() {
        assertThat(cidrMatcher.matches("192.168.1.50", "192.168.1.50/32")).isTrue();
        assertThat(cidrMatcher.matches("192.168.1.51", "192.168.1.50/32")).isFalse();
    }

    @Test
    @DisplayName("Should match single IPv4 rule without explicit prefix (normalized to /32)")
    void shouldMatchSingleIpv4WithoutPrefix() {
        assertThat(cidrMatcher.normalizeCidrOrIp("192.168.1.50")).isEqualTo("192.168.1.50/32");
        assertThat(cidrMatcher.matches("192.168.1.50", "192.168.1.50")).isTrue();
        assertThat(cidrMatcher.matches("192.168.1.51", "192.168.1.50")).isFalse();
    }

    @Test
    @DisplayName("Should match all addresses in IPv4 /24 range including network and broadcast")
    void shouldMatchIpv4Slash24Range() {
        String cidr = "192.168.1.0/24";
        assertThat(cidrMatcher.matches("192.168.1.0", cidr)).isTrue();
        assertThat(cidrMatcher.matches("192.168.1.1", cidr)).isTrue();
        assertThat(cidrMatcher.matches("192.168.1.50", cidr)).isTrue();
        assertThat(cidrMatcher.matches("192.168.1.255", cidr)).isTrue();
        assertThat(cidrMatcher.matches("192.168.2.1", cidr)).isFalse();
        assertThat(cidrMatcher.matches("10.0.0.1", cidr)).isFalse();
    }

    @Test
    @DisplayName("Should match IPv6 CIDR range (e.g. 2001:db8::/32)")
    void shouldMatchIpv6CidrRange() {
        String cidr = "2001:db8::/32";
        assertThat(cidrMatcher.matches("2001:db8::1", cidr)).isTrue();
        assertThat(cidrMatcher.matches("2001:db8:1234:5678::abcd", cidr)).isTrue();
        assertThat(cidrMatcher.matches("2001:db9::1", cidr)).isFalse();
        assertThat(cidrMatcher.matches("2001:4860:4860::8888", cidr)).isFalse();
    }

    @Test
    @DisplayName("Should match single IPv6 address normalized to /128")
    void shouldMatchSingleIpv6Address() {
        assertThat(cidrMatcher.normalizeCidrOrIp("2001:db8::1")).isEqualTo("2001:db8::1/128");
        assertThat(cidrMatcher.matches("2001:db8:0:0:0:0:0:1", "2001:db8::1")).isTrue();
        assertThat(cidrMatcher.matches("2001:db8::2", "2001:db8::1")).isFalse();
    }

    @Test
    @DisplayName("Should return false when comparing IPv4 client with IPv6 rule and vice versa")
    void shouldNotMatchDifferentAddressFamilies() {
        assertThat(cidrMatcher.matches("192.168.1.50", "2001:db8::/32")).isFalse();
        assertThat(cidrMatcher.matches("2001:db8::1", "192.168.1.0/24")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "   ",
        "invalid-cidr",
        "999.999.999.999/24",
        "192.168.1.0/33",
        "192.168.1.0/-1",
        "192.168.1.0/abc",
        "2001:db8::/129",
        "2001:db8::/-5",
        "192.168.1.1/24/32"
    })
    @DisplayName("Should throw InvalidIpException for invalid IP or CIDR rules")
    void shouldRejectInvalidCidrOrIp(String invalidRule) {
        assertThatThrownBy(() -> cidrMatcher.normalizeCidrOrIp(invalidRule))
            .isInstanceOf(InvalidIpException.class);
    }
}
