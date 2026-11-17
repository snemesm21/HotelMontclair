package com.example.demo.controller;

import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.entities.Client;
import com.example.demo.security.JWTGenerator;
import com.example.demo.service.ClientService;
import com.example.demo.service.GoogleOAuthService;
import com.example.demo.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private AuthController controller;
    private ClientService clientService;
    private JwtService jwtService;
    private AuthenticationManager authenticationManager;
    private JWTGenerator jwtGenerator;

    @BeforeEach
    void setUp() {
        controller = new AuthController();
        clientService = mock(ClientService.class);
        jwtService = mock(JwtService.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtGenerator = mock(JWTGenerator.class);
        ReflectionTestUtils.setField(controller, "clientService", clientService);
        ReflectionTestUtils.setField(controller, "googleOAuthService", mock(GoogleOAuthService.class));
        ReflectionTestUtils.setField(controller, "jwtService", jwtService);
        ReflectionTestUtils.setField(controller, "authenticationManager", authenticationManager);
        ReflectionTestUtils.setField(controller, "jwtGenerator", jwtGenerator);
        ReflectionTestUtils.setField(controller, "cookieSecure", false);
        when(jwtService.createToken(1L, "demo@example.com", "CLIENT")).thenReturn("jwt");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("demo", null));
        when(jwtGenerator.generateToken(any(Authentication.class))).thenReturn("jwt-token");
    }

    @Test
    void traditionalLoginContractRemainsCompatible() {
        Client client = Client.builder().id(1L).username("demo").password("demo")
                .email("demo@example.com").firstName("Demo").lastName("User")
                .role("CLIENT").authProvider("LOCAL").build();
        when(clientService.findByUsername("demo")).thenReturn(client);

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.identifier = "demo";
        request.password = "demo";
        var response = mock(HttpServletResponse.class);

        var result = controller.login(request, response);
        AuthResponseDTO body = result.getBody();

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("jwt-token", body.getToken());
        assertEquals("/profile", body.getRedirectUrl());
    }
}
