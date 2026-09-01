package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String phone;
    private String role;

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(username);
    }

    public String getNombre() {
        if (firstName != null && !firstName.isBlank()) {
            return (lastName != null && !lastName.isBlank()) ? firstName + " " + lastName : firstName;
        }
        return username != null ? username : "Cliente";
    }

    public String getFotoUrl() {
        return (avatarUrl != null && !avatarUrl.isBlank()) ? avatarUrl : "/Images/avatar-default.png";
    }
}
