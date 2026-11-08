package com.example.demo.controller;

import com.example.demo.service.GoogleOAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class GoogleOAuthCallbackController {

    private final GoogleOAuthService googleOAuthService;
    private final String frontendUrl;

    public GoogleOAuthCallbackController(GoogleOAuthService googleOAuthService,
                                         @Value("${hotelmontclair.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.googleOAuthService = googleOAuthService;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/login/oauth2/code/google")
    public String callback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String state,
                           @RequestParam(required = false) String error,
                           @RequestParam(name = "error_description", required = false) String errorDescription) {
        if (state == null || state.isBlank()) {
            return redirectToFrontendError("El callback de Google no contiene state.");
        }
        try {
            String exchangeCode = googleOAuthService.completeAuthorization(code, state, error);
            return "redirect:" + UriComponentsBuilder
                    .fromUriString(frontendUrl + "/auth/google/callback")
                    .queryParam("code", exchangeCode)
                    .build().encode().toUriString();
        } catch (IllegalArgumentException | IllegalStateException e) {
            String message = errorDescription != null && !errorDescription.isBlank()
                    ? errorDescription : e.getMessage();
            return redirectToFrontendError(message);
        }
    }

    private String redirectToFrontendError(String message) {
        return "redirect:" + UriComponentsBuilder
                .fromUriString(frontendUrl + "/auth/google/callback")
                .queryParam("error", message == null ? "No fue posible iniciar sesión con Google." : message)
                .build().encode().toUriString();
    }
}
