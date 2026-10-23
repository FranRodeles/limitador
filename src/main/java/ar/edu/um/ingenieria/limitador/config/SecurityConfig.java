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
import ar.edu.um.ingenieria.limitador.security.UnauthorizedAccessHandler;

@Configuration
public class SecurityConfig {

    private final org.springframework.beans.factory.ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider;
    private final org.springframework.beans.factory.ObjectProvider<JwtAuthenticationEntryPoint> jwtAuthenticationEntryPointProvider;
    private final org.springframework.beans.factory.ObjectProvider<UnauthorizedAccessHandler> unauthorizedAccessHandlerProvider;

    public SecurityConfig(org.springframework.beans.factory.ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider,
                          org.springframework.beans.factory.ObjectProvider<JwtAuthenticationEntryPoint> jwtAuthenticationEntryPointProvider,
                          org.springframework.beans.factory.ObjectProvider<UnauthorizedAccessHandler> unauthorizedAccessHandlerProvider) {
        this.jwtAuthenticationFilterProvider = jwtAuthenticationFilterProvider;
        this.jwtAuthenticationEntryPointProvider = jwtAuthenticationEntryPointProvider;
        this.unauthorizedAccessHandlerProvider = unauthorizedAccessHandlerProvider;
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
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/users/login", "/api/users/register").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/search").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/users").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/users/{id}").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/{id}").hasAnyRole("USER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/users/{id}").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/roles/**").hasRole("ADMIN")
                .requestMatchers("/api/users-data/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );

        JwtAuthenticationEntryPoint entryPoint = jwtAuthenticationEntryPointProvider.getIfAvailable();
        UnauthorizedAccessHandler accessDeniedHandler = unauthorizedAccessHandlerProvider.getIfAvailable();
        http.exceptionHandling(ex -> {
            if (entryPoint != null) {
                ex.authenticationEntryPoint(entryPoint);
            }
            if (accessDeniedHandler != null) {
                ex.accessDeniedHandler(accessDeniedHandler);
            }
        });

        JwtAuthenticationFilter filter = jwtAuthenticationFilterProvider.getIfAvailable();
        if (filter != null) {
            http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
