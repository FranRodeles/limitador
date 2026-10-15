package ar.edu.um.ingenieria.limitador.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ar.edu.um.ingenieria.limitador.security.JwtAuthenticationEntryPoint;
import ar.edu.um.ingenieria.limitador.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final org.springframework.beans.factory.ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider;
    private final org.springframework.beans.factory.ObjectProvider<JwtAuthenticationEntryPoint> jwtAuthenticationEntryPointProvider;

    public SecurityConfig(org.springframework.beans.factory.ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider,
                          org.springframework.beans.factory.ObjectProvider<JwtAuthenticationEntryPoint> jwtAuthenticationEntryPointProvider) {
        this.jwtAuthenticationFilterProvider = jwtAuthenticationFilterProvider;
        this.jwtAuthenticationEntryPointProvider = jwtAuthenticationEntryPointProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean({
        ar.edu.um.ingenieria.limitador.security.JwtService.class,
        org.springframework.security.core.userdetails.UserDetailsService.class
    })
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            ar.edu.um.ingenieria.limitador.security.JwtService jwtService,
            org.springframework.security.core.userdetails.UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        JwtAuthenticationEntryPoint entryPoint = jwtAuthenticationEntryPointProvider.getIfAvailable();
        if (entryPoint != null) {
            http.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint));
        }

        JwtAuthenticationFilter filter = jwtAuthenticationFilterProvider.getIfAvailable();
        if (filter != null) {
            http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
