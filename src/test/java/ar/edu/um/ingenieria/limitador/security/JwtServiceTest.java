package ar.edu.um.ingenieria.limitador.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;
import tools.jackson.databind.ObjectMapper;

class JwtServiceTest {

    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long NORMAL_EXPIRATION_MS = 86400000L; // 24 hours
    private static final long REMEMBER_ME_EXPIRATION_MS = 31536000000L; // 365 days

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_KEY, NORMAL_EXPIRATION_MS, REMEMBER_ME_EXPIRATION_MS);
        userDetails = User.builder()
            .username("john_doe")
            .password("password123")
            .roles("USER")
            .build();
    }

    @Test
    @DisplayName("Should generate token containing only username and exp claims")
    void shouldGenerateTokenWithOnlyUsernameAndExpClaims() throws Exception {
        String token = jwtService.generateToken("john_doe", false);
        assertThat(token).isNotBlank();

        // Decode payload JSON directly from JWT second segment
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> claimsMap = mapper.readValue(payloadJson, Map.class);

        // Verify ONLY username and exp exist in payload
        assertThat(claimsMap).containsOnlyKeys("username", "exp");
        assertThat(claimsMap.get("username")).isEqualTo("john_doe");
        assertThat(claimsMap.get("exp")).isNotNull();

        // Technical standard claims check
        assertThat(claimsMap).doesNotContainKeys("sub", "iss", "aud", "iat", "jti", "roles", "id", "password", "email");
    }

    @Test
    @DisplayName("Should generate token with normal duration when rememberMe is false")
    void shouldGenerateTokenWithNormalDurationWhenRememberMeIsFalse() {
        Instant before = Instant.now();
        String token = jwtService.generateToken("john_doe", false);
        Date expiration = jwtService.extractExpiration(token);

        Instant expectedMin = before.plus(23, ChronoUnit.HOURS);
        Instant expectedMax = before.plus(25, ChronoUnit.HOURS);

        assertThat(expiration.toInstant()).isBetween(expectedMin, expectedMax);
    }

    @Test
    @DisplayName("Should generate token with 1 year duration when rememberMe is true")
    void shouldGenerateTokenWithOneYearDurationWhenRememberMeIsTrue() {
        Instant before = Instant.now();
        String token = jwtService.generateToken("john_doe", true);
        Date expiration = jwtService.extractExpiration(token);

        Instant expectedMin = before.plus(364, ChronoUnit.DAYS);
        Instant expectedMax = before.plus(366, ChronoUnit.DAYS);

        assertThat(expiration.toInstant()).isBetween(expectedMin, expectedMax);
    }

    @Test
    @DisplayName("Should extract username correctly from valid token")
    void shouldExtractUsernameFromValidToken() {
        String token = jwtService.generateToken("john_doe", false);
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("Should validate token successfully for matching user")
    void shouldValidateTokenForMatchingUser() {
        String token = jwtService.generateToken(userDetails.getUsername(), false);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("Should invalidate token when username does not match")
    void shouldInvalidateTokenWhenUsernameDoesNotMatch() {
        String token = jwtService.generateToken("another_user", false);

        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    @DisplayName("Should reject manipulated token due to signature mismatch")
    void shouldRejectManipulatedToken() {
        String token = jwtService.generateToken("john_doe", false);
        String[] parts = token.split("\\.");
        // Alter the payload segment
        String tamperedToken = parts[0] + "." + parts[1] + "extra." + parts[2];

        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
        assertThat(jwtService.isTokenValid(tamperedToken, userDetails)).isFalse();
    }

    @Test
    @DisplayName("Should reject token when signature is altered")
    void shouldRejectTokenWhenSignatureIsAltered() {
        String token = jwtService.generateToken("john_doe", false);
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".invalidSignature";

        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // JwtService configured with negative duration to produce expired token
        JwtService expiredJwtService = new JwtService(SECRET_KEY, -1000L, -1000L);
        String expiredToken = expiredJwtService.generateToken("john_doe", false);

        assertThat(jwtService.validateToken(expiredToken)).isFalse();
        assertThat(jwtService.isTokenValid(expiredToken, userDetails)).isFalse();
    }

    @Test
    @DisplayName("Should fail when secret key is null or too short (< 32 bytes)")
    void shouldFailWhenSecretKeyIsTooShort() {
        assertThatThrownBy(() -> new JwtService("short-secret", NORMAL_EXPIRATION_MS, REMEMBER_ME_EXPIRATION_MS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 256 bits");
    }
}
