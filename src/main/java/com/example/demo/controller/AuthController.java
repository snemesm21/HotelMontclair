package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.security.JWTGenerator;
import com.example.demo.service.ClientService;
import com.example.demo.service.GoogleOAuthService;
import com.example.demo.service.JwtService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

//Controlador REST de autenticación (/api/auth).
@RestController
@RequestMapping({"/api/auth"})
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AuthController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTGenerator jwtGenerator;

    @Value("${hotelmontclair.auth.cookie-secure:false}")
    private boolean cookieSecure;

    //Cuerpo esperado en la petición de login.
    public static class LoginRequest {
        public String identifier;
        public String password;
    }

    /*Inicia sesión (POST /api/auth/login). Devuelve el JWT (header Authorization en las siguientes peticiones)
    y además deja la cookie de sesión propia para el resto de flujos (Google, e2e).*/
    @Operation(summary = "Inicia sesión con usuario o correo y contraseña; devuelve un JWT")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse response) {
        if (req == null || req.identifier == null || req.password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Credenciales incompletas."));
        }
        Client client = clientService.login(req.identifier, req.identifier, req.password);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Usuario o contraseña incorrectos."));
        }

        // El identificador puede ser correo o usuario; para autenticar se usa siempre el username real
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(client.getUsername(), req.password.trim()));
        String token = jwtGenerator.generateToken(authentication);

        setSessionCookie(response, client);

        String redirectUrl = client.isAdmin()
                ? "/admin/rooms"
                : (client.isOperator() ? "/operator/reservations" : "/profile");
        return ResponseEntity.ok(Map.of(
                "success", true,
                "token", token,
                "client", client,
                "role", client.getRole(),
                "redirectUrl", redirectUrl));
    }

    @Operation(summary = "Inicia el flujo OAuth2 Authorization Code con PKCE para Google")
    @GetMapping("/google/start")
    public ResponseEntity<?> startGoogleLogin() {
        try {
            return ResponseEntity.ok(Map.of(
                    "authorizationUrl",
                    googleOAuthService.createAuthorizationUrl()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "Intercambia un código temporal de un solo uso por la sesión propia")
    @PostMapping("/google/exchange")
    public ResponseEntity<?> exchangeGoogleCode(@RequestBody Map<String, String> body,
                                                HttpServletResponse response) {
        String code = body == null ? null : body.get("code");
        try {
            Client client = googleOAuthService.exchangeTemporaryCode(code);
            setSessionCookie(response, client);
            String redirectUrl = "/profile/" + client.getId();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "client", client,
                    "role", client.getRole(),
                    "redirectUrl", redirectUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "Consulta el cliente autenticado (JWT por header o cookie propia)")
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        Optional<JwtService.JwtClaims> claims = currentClaims(request);
        if (claims.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No hay una sesión activa."));
        }
        Client client = clientService.findById(claims.get().clientId());
        String redirectUrl = "ADMIN".equalsIgnoreCase(client.getRole())
                ? "/admin/rooms"
                : ("OPERATOR".equalsIgnoreCase(client.getRole()) || "OPERARIO".equalsIgnoreCase(client.getRole())
                        ? "/operator/reservations"
                        : "/profile");
        return ResponseEntity.ok(Map.of(
                "success", true,
                "client", client,
                "role", client.getRole(),
                "redirectUrl", redirectUrl));
    }

    @Operation(summary = "Cierra la sesión propia (borra la cookie; el front borra su JWT)")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("HM_SESSION", "")
                .httpOnly(true).secure(cookieSecure).sameSite("Lax")
                .path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("success", true, "message", "Sesión cerrada."));
    }

    @Operation(summary = "Inicializa el token CSRF para solicitudes mutantes desde Angular")
    @GetMapping("/csrf")
    public ResponseEntity<?> csrf(CsrfToken csrfToken, HttpServletRequest request) {
        if (csrfToken == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "No fue posible inicializar CSRF."));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName(),
                "token", csrfToken.getToken()));
    }

    // Primero lo que dejó el filtro (Bearer o cookie ya procesada); si no, se lee la cookie directamente
    private Optional<JwtService.JwtClaims> currentClaims(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtService.JwtClaims claims) {
            return Optional.of(claims);
        }
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

    private void setSessionCookie(HttpServletResponse response, Client client) {
        String jwt = jwtService.createToken(client.getId(), client.getEmail(), client.getRole());
        ResponseCookie cookie = ResponseCookie.from("HM_SESSION", jwt)
                .httpOnly(true).secure(cookieSecure).sameSite("Lax")
                .path("/").maxAge(7200).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}