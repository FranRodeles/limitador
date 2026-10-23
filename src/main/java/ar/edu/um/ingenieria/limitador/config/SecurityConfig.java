package ar.edu.um.ingenieria.limitador.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import ar.edu.um.ingenieria.limitador.security.JwtAuthenticationEntryPoint;
import ar.edu.um.ingenieria.limitador.security.JwtAuthenticationFilter;
import ar.edu.um.ingenieria.limitador.security.UnauthorizedAccessHandler;


@Configuration
@EnableMethodSecurity
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
            .cors(corsCustomizer -> corsCustomizer.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
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

    @Bean 
    CorsConfigurationSource corsConfigurationSource(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.applyPermitDefaultValues();
        configuration.setAllowCredentials(false);
        configuration.setAllowedOrigins(Arrays.asList("*")); //Agregar valores de dominio permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        source.registerCorsConfiguration("/**", configuration);
        return request -> configuration;

    }
}
