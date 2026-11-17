package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO tipado para respuestas de autenticación (/api/auth/login, /api/auth/me, /api/auth/logout).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseDTO {

    private boolean success;
    private String token;
    private ClientDTO client;
    private String role;
    private String redirectUrl;
    private String message;
}
