package ar.edu.um.ingenieria.limitador.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import ar.edu.um.ingenieria.limitador.validator.CidrMatcher;

@DisplayName("ClientIpResolver Specification")
class ClientIpResolverTest {

    private CidrMatcher cidrMatcher;
    private ClientIpResolver clientIpResolver;

    @BeforeEach
    void setUp() {
        cidrMatcher = new CidrMatcher();
        clientIpResolver = new ClientIpResolver(
            cidrMatcher,
            List.of("127.0.0.1/32", "::1/128", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16")
        );
    }

    @Test
    @DisplayName("Should return remoteAddr directly when no proxy headers are present")
    void shouldReturnRemoteAddrWhenNoHeadersPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("181.45.20.10");

        assertThat(clientIpResolver.resolveClientIp(request)).isEqualTo("181.45.20.10");
    }

    @Test
    @DisplayName("Should extract client IP from X-Forwarded-For when request comes from a trusted proxy")
    void shouldExtractClientIpFromXForwardedForBehindTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.50, 10.0.0.5");

        assertThat(clientIpResolver.resolveClientIp(request)).isEqualTo("203.0.113.50");
    }

    @Test
    @DisplayName("Should extract client IP from X-Real-IP when behind trusted proxy and X-Forwarded-For is absent")
    void shouldExtractClientIpFromXRealIpBehindTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Real-IP", "198.51.100.77");

        assertThat(clientIpResolver.resolveClientIp(request)).isEqualTo("198.51.100.77");
    }

    @Test
    @DisplayName("Should ignore spoofed X-Forwarded-For and X-Real-IP when remoteAddr is NOT a trusted proxy")
    void shouldIgnoreSpoofedHeadersFromUntrustedRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.99");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        request.addHeader("X-Real-IP", "5.6.7.8");

        assertThat(clientIpResolver.resolveClientIp(request)).isEqualTo("198.51.100.99");
    }

    @Test
    @DisplayName("Should resist right-to-left X-Forwarded-For spoofing when client appends fake IP before trusted proxy")
    void shouldTakeFirstUntrustedIpFromRightInXForwardedForChain() {
        // Client (198.51.100.200) sends spoofed header "X-Forwarded-For: 8.8.8.8"
        // Trusted load balancer (10.0.0.2) appends real client IP -> "8.8.8.8, 198.51.100.200"
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "8.8.8.8, 198.51.100.200");

        assertThat(clientIpResolver.resolveClientIp(request)).isEqualTo("198.51.100.200");
    }

    @Test
    @DisplayName("Should ignore malformed IP values in X-Forwarded-For and fall back safely")
    void shouldIgnoreMalformedXForwardedForHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "not-an-ip; DROP TABLE users;--");

        assertThat(clientIpResolver.resolveClientIp(request)).isEqualTo("127.0.0.1");
    }
}
