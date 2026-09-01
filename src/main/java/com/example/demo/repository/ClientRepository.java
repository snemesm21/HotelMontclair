package com.example.demo.repository;

import com.example.demo.entities.Client;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ClientRepository {
    private final Map<Long, Client> store = new LinkedHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @PostConstruct
    public void init() {
        // seed an admin user (username: admin, password: admin)
        save(new Client(null, "admin", "admin", "admin@hotelmontclair.com", "Administrador", "Montclair", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80", "+34 910 000 001", "ADMIN"));

        // seed a demo client (username: demo, password: demo)
        save(new Client(null, "demo", "demo", "demo@example.com", "Demo", "User", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80", "+34 612 345 678", "CLIENT"));
    }

    public List<Client> findAll() { return new ArrayList<>(store.values()); }
    public Client findById(Long id) { return store.get(id); }
    public Client save(Client c) {
        if (c.getId() == null) c.setId(idGen.getAndIncrement());
        if (c.getUsername() != null) c.setUsername(c.getUsername().trim());
        if (c.getEmail() != null) c.setEmail(c.getEmail().trim().toLowerCase());
        if (c.getPassword() != null) c.setPassword(c.getPassword().trim());
        if (c.getAvatarUrl() == null || c.getAvatarUrl().isBlank()) {
            c.setAvatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80");
        }
        if (c.getRole() == null || c.getRole().isBlank()) {
            if ("admin".equalsIgnoreCase(c.getUsername())) {
                c.setRole("ADMIN");
            } else {
                c.setRole("CLIENT");
            }
        }
        store.put(c.getId(), c);
        return c;
    }
    public void delete(Long id) { store.remove(id); }

    public Optional<Client> findByUsername(String username) {
        if (username == null) return Optional.empty();
        String trimmed = username.trim();
        return store.values().stream()
                .filter(c -> c.getUsername() != null && c.getUsername().trim().equalsIgnoreCase(trimmed))
                .findFirst();
    }

    public Optional<Client> findByUsernameOrEmail(String identifier) {
        if (identifier == null) return Optional.empty();
        String trimmed = identifier.trim().toLowerCase();
        return store.values().stream()
                .filter(c -> (c.getUsername() != null && c.getUsername().trim().toLowerCase().equals(trimmed))
                        || (c.getEmail() != null && c.getEmail().trim().toLowerCase().equals(trimmed)))
                .findFirst();
    }
}
