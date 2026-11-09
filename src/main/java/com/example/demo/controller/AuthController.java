package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.web.csrf.CsrfToken;

import com.example.demo.service.GoogleOAuthService;
import com.example.demo.service.JwtService;

/*Controlador REST de autenticación (/api/auth).
No usa tokens: si las credenciales son correctas devuelve el cliente completo
y el frontend lo guarda como sesión.*/
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

    @Value("${hotelmontclair.auth.cookie-secure:false}")
    private boolean cookieSecure;

    //Cuerpo esperado en la petición de login.
    public static class LoginRequest {
        public String identifier;
        public String password;
    }

    /*Inicia sesión (POST /api/auth/login). El mismo valor de `identifier` se envía al servicio como usuario y como correo,
    así que el cliente puede entrar con cualquiera de los dos.*/
    @Operation (summary = "Inicia sesión con correo electrónico o número de teléfono y contraseña")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse response) {
        if (req == null || req.identifier == null || req.password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Credenciales incompletas."));
        }
        Client client = clientService.login(req.identifier, req.identifier, req.password);
        if (client != null) {
            //Los administradores van al panel de habitaciones; los operarios al portal de operador; los clientes, a su perfil.
            String redirectUrl = client.isAdmin()
                    ? "/admin/rooms"
                    : (client.isOperator() ? "/operator/reservations" : "/profile/" + client.getId());
            setSessionCookie(response, client);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "client", client,
                "role", client.getRole(),
                "redirectUrl", redirectUrl
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Usuario o contraseña incorrectos."));
        }
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

    @Operation(summary = "Consulta el cliente autenticado usando la cookie HttpOnly propia")
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        Optional<JwtService.JwtClaims> claims = jwtClaims(request);
        if (claims.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No hay una sesión activa."));
        }
        Client client = clientService.findById(claims.get().clientId());
        String redirectUrl = "ADMIN".equalsIgnoreCase(client.getRole())
                ? "/admin/rooms"
                : ("OPERATOR".equalsIgnoreCase(client.getRole()) || "OPERARIO".equalsIgnoreCase(client.getRole())
                        ? "/operator/reservations"
                        : "/profile/" + client.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "client", client,
                "role", client.getRole(),
                "redirectUrl", redirectUrl));
    }

    @Operation(summary = "Cierra la sesión propia")
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

    private Optional<JwtService.JwtClaims> jwtClaims(HttpServletRequest request) {
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
