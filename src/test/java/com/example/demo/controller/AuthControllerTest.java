package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import com.example.demo.service.GoogleOAuthService;
import com.example.demo.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private AuthController controller;
    private ClientService clientService;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        controller = new AuthController();
        clientService = mock(ClientService.class);
        jwtService = mock(JwtService.class);
        ReflectionTestUtils.setField(controller, "clientService", clientService);
        ReflectionTestUtils.setField(controller, "googleOAuthService", mock(GoogleOAuthService.class));
        ReflectionTestUtils.setField(controller, "jwtService", jwtService);
        ReflectionTestUtils.setField(controller, "cookieSecure", false);
        when(jwtService.createToken(1L, "demo@example.com", "CLIENT")).thenReturn("jwt");
    }

    @Test
    void traditionalLoginContractRemainsCompatible() {
        Client client = Client.builder().id(1L).username("demo").password("demo")
                .email("demo@example.com").firstName("Demo").lastName("User")
                .role("CLIENT").authProvider("LOCAL").build();
        when(clientService.login("demo", "demo", "demo")).thenReturn(client);

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.identifier = "demo";
        request.password = "demo";
        var response = mock(HttpServletResponse.class);

        var result = controller.login(request, response);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(true, ((java.util.Map<?, ?>) result.getBody()).get("success"));
        assertEquals("/profile/1", ((java.util.Map<?, ?>) result.getBody()).get("redirectUrl"));
    }
}
