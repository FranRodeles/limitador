package ar.edu.um.ingenieria.limitador.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldProvidePasswordEncoderBean() {
        PasswordEncoder encoder = applicationContext.getBean(PasswordEncoder.class);

        assertThat(encoder).isNotNull();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void shouldEncodePassword() {
        PasswordEncoder encoder = applicationContext.getBean(PasswordEncoder.class);
        String rawPassword = "mySecretPassword";

        String encoded = encoder.encode(rawPassword);

        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoded).startsWith("$2a$");
    }

    @Test
    void shouldVerifyEncodedPassword() {
        PasswordEncoder encoder = applicationContext.getBean(PasswordEncoder.class);
        String rawPassword = "mySecretPassword";
        String encoded = encoder.encode(rawPassword);

        boolean matches = encoder.matches(rawPassword, encoded);

        assertThat(matches).isTrue();
    }
}
