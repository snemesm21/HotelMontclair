package com.example.demo.dto;

import com.example.demo.entities.Client;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO plano de cliente para respuestas REST.
 * Expone únicamente los datos necesarios para el frontend sin relaciones ni credenciales internas.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String phone;
    private String role;
    private String authProvider;

    public static ClientDTO fromEntity(Client client) {
        if (client == null) {
            return null;
        }
        return ClientDTO.builder()
                .id(client.getId())
                .username(client.getUsername())
                .email(client.getEmail())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .avatarUrl(client.getAvatarUrl())
                .phone(client.getPhone())
                .role(client.getRole())
                .authProvider(client.getAuthProvider())
                .build();
    }
}
