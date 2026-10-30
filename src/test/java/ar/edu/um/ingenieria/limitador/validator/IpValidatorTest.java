package ar.edu.um.ingenieria.limitador.validator;

import java.net.InetAddress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.edu.um.ingenieria.limitador.exception.InvalidIpException;
import ar.edu.um.ingenieria.limitador.exception.PrivateIpAddressException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IpValidator Specification")
class IpValidatorTest {

    private IpValidator ipValidator;

    @BeforeEach
    void setUp() {
        ipValidator = new IpValidator();
    }

    @Test
    @DisplayName("should accept valid public IPv4 address")
    void shouldAcceptValidPublicIpv4() {
        InetAddress address = ipValidator.validateAndParse("8.8.8.8");

        assertThat(address).isNotNull();
        assertThat(address.getHostAddress()).isEqualTo("8.8.8.8");
    }

    @Test
    @DisplayName("should accept valid public IPv6 address")
    void shouldAcceptValidPublicIpv6() {
        InetAddress address = ipValidator.validateAndParse("2001:4860:4860::8888");

        assertThat(address).isNotNull();
        assertThat(address.getHostAddress()).isEqualTo("2001:4860:4860:0:0:0:0:8888");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("should throw InvalidIpException for null or blank IP strings")
    void shouldRejectBlankIp(String blankIp) {
        assertThatThrownBy(() -> ipValidator.validateAndParse(blankIp))
                .isInstanceOf(InvalidIpException.class)
                .hasMessageContaining("vacía");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-ip", "999.999.999.999", "1.2.3", "1.2.3.4.5", "256.1.1.1", "abc:def:xyz"})
    @DisplayName("should throw InvalidIpException for malformed IP strings")
    void shouldRejectMalformedIp(String invalidIp) {
        assertThatThrownBy(() -> ipValidator.validateAndParse(invalidIp))
                .isInstanceOf(InvalidIpException.class)
                .hasMessageContaining("inválida");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "127.0.0.1",          // Loopback IPv4
        "::1",                // Loopback IPv6
        "10.0.0.1",           // Private RFC 1918 Class A
        "172.16.0.1",         // Private RFC 1918 Class B
        "192.168.1.1",        // Private RFC 1918 Class C
        "169.254.1.1",        // Link-local
        "0.0.0.0"             // Any local address
    })
    @DisplayName("should throw PrivateIpAddressException for private or loopback IP addresses")
    void shouldRejectPrivateOrLocalIp(String privateIp) {
        assertThatThrownBy(() -> ipValidator.validateAndParse(privateIp))
                .isInstanceOf(PrivateIpAddressException.class)
                .hasMessageContaining("privada o local");
    }
}
