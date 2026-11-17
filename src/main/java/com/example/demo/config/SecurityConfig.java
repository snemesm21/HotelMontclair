package com.example.demo.config;

import com.example.demo.security.JWTAuthenticationFilter;
import com.example.demo.security.JwtAuthEntryPoint;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final JwtAuthEntryPoint authEntryPoint;

    public SecurityConfig(SessionAuthenticationFilter sessionAuthenticationFilter,
                          JwtAuthEntryPoint authEntryPoint) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
        this.authEntryPoint = authEntryPoint;
    }

    // Consola H2 (solo perfil dev)
    @Bean
    @Profile("dev")
    @Order(1)
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/h2-console", "/h2-console/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // Arquitectura cliente-servidor separada (Angular + Spring Security JWT): CSRF desactivado
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint(authEntryPoint))
                // Primero el JWT por header (video); si no hay, el filtro de cookie de la sesión propia
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter(), SessionAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/ai/**",
                                "/login/**",
                                "/h2-console/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()

                        // Restricción por roles (JWT del video y sesión HM_SESSION)
                        .requestMatchers(HttpMethod.GET, "/api/clients").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/clients/*").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/reservations",
                                "/api/reservations/summary").hasAnyAuthority("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/reservations/*/activate",
                                "/api/reservations/*/finish").hasAnyAuthority("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST,
                                "/api/reservations/rooms/*/services",
                                "/api/rooms/*/services").hasAnyAuthority("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/reservations/*/services/*").hasAnyAuthority("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/room/*/active")
                        .hasAnyAuthority("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST,
                                "/api/rooms", "/api/room-types", "/api/services").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/rooms/*", "/api/room-types/*", "/api/services/*", "/api/services")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/rooms/*", "/api/room-types/*", "/api/services/*").hasAuthority("ADMIN")

                        // Catálogo público y registro
                        .requestMatchers(HttpMethod.GET,
                                "/api/rooms",
                                "/api/rooms/**",
                                "/api/room-types",
                                "/api/room-types/**",
                                "/api/services",
                                "/api/services/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/clients").permitAll()
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public JWTAuthenticationFilter jwtAuthenticationFilter() {
        return new JWTAuthenticationFilter();
    }
}