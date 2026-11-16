package com.example.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.DispatcherType;

@Configuration 
@EnableWebSecurity 
public class SecurityConfig {

    @Autowired 
    private JwtAuthEntryPoint authEntryPoint;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(e -> e.authenticationEntryPoint(authEntryPoint))
            .headers(h -> h.frameOptions(f -> f.disable()))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                // Públicos
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/h2-console/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/clients").permitAll()   // registro
                .requestMatchers(HttpMethod.GET, "/api/clients/details").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/clients/*").hasAnyAuthority("ADMIN", "OPERATOR")

                // Personal del hotel: ADMIN u OPERATOR
                .requestMatchers(HttpMethod.PATCH,
                        "/api/reservations/*/activate",
                        "/api/reservations/*/pay",
                        "/api/reservations/*/finish").hasAnyAuthority("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.POST,
                        "/api/reservations/rooms/*/services",
                        "/api/rooms/*/services").hasAnyAuthority("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/reservations/*/services/*").hasAnyAuthority("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.GET, "/api/reservations/room/*/active").hasAnyAuthority("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.GET, "/api/reservations", "/api/reservations/summary").hasAnyAuthority("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.GET, "/api/clients").hasAnyAuthority("ADMIN", "OPERATOR")

                // Solo ADMIN
                .requestMatchers(HttpMethod.DELETE, "/api/clients/*").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/rooms", "/api/room-types", "/api/services").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/rooms/*", "/api/room-types/*", "/api/services/*", "/api/services").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/rooms/*", "/api/room-types/*", "/api/services/*").hasAuthority("ADMIN")

                // Catálogo público (solo lectura)
                .requestMatchers(HttpMethod.GET, "/api/rooms/**", "/api/room-types/**", "/api/services/**").permitAll()

                // Todo lo demás: cualquier usuario autenticado
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean 
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean 
    AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean 
    JWTAuthenticationFilter jwtAuthenticationFilter() {
        return new JWTAuthenticationFilter();
    }
}
