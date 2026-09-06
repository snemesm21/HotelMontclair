package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clients")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(length = 80)
    private String lastName;

    @Column(length = 500)
    private String avatarUrl;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 20)
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
