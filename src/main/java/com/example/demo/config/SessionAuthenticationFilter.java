package com.example.demo.config;

import com.example.demo.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public SessionAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Optional<JwtService.JwtClaims> claims = readClaims(request);
        if (claims.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
            JwtService.JwtClaims value = claims.get();
            String role = normalizeRole(value.role());
            var authentication = new UsernamePasswordAuthenticationToken(
                    value,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Optional<JwtService.JwtClaims> readClaims(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if ("HM_SESSION".equals(cookie.getName())) {
                return jwtService.validateToken(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private String normalizeRole(String role) {
        if ("ADMIN".equalsIgnoreCase(role)) return "ADMIN";
        if ("OPERATOR".equalsIgnoreCase(role) || "OPERARIO".equalsIgnoreCase(role)) return "OPERATOR";
        return "CLIENT";
    }
}
