package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Getter
@Setter
@ToString
@Builder
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

    public boolean isOperator() {
        return "OPERATOR".equalsIgnoreCase(role) || "OPERARIO".equalsIgnoreCase(role) || "operario".equalsIgnoreCase(username);
    }
}
