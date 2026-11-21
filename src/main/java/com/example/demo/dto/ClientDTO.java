package com.example.demo.dto;

import com.example.demo.entities.Client;
import com.example.demo.entities.LoyaltyTier;
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
    private Integer points;
    private Integer lifetimePoints;
    private String loyaltyTier;
    private String loyaltyTierLabel;

    public static ClientDTO fromEntity(Client client) {
        if (client == null) {
            return null;
        }
        int safePoints = client.getPoints() != null ? client.getPoints() : 0;
        int safeLifetime = client.getLifetimePoints() != null ? client.getLifetimePoints() : 0;
        LoyaltyTier tier = client.getLoyaltyTier() != null
                ? client.getLoyaltyTier()
                : LoyaltyTier.fromLifetimePoints(safeLifetime);

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
                .points(safePoints)
                .lifetimePoints(safeLifetime)
                .loyaltyTier(tier.name())
                .loyaltyTierLabel(tier.getDisplayName())
                .build();
    }
}
