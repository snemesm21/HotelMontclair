package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

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

    @Transient 
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

    @OneToOne(cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private UserEntity user;

    public Client(Long id, String username, String password, String email, String firstName,
                  String lastName, String avatarUrl, String phone, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatarUrl = avatarUrl;
        this.phone = phone;
        this.role = role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(username);
    }

    public boolean isOperator() {
        return "OPERATOR".equalsIgnoreCase(role) || "OPERARIO".equalsIgnoreCase(role) || "operario".equalsIgnoreCase(username);
    }
}
