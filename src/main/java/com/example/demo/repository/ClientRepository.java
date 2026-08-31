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
        // seed a demo client (username: demo, password: demo)
        save(new Client(null, "demo", "demo", "demo@example.com", "Demo", "User"));
    }

    public List<Client> findAll() { return new ArrayList<>(store.values()); }
    public Client findById(Long id) { return store.get(id); }
    public Client save(Client c) {
        if (c.getId() == null) c.setId(idGen.getAndIncrement());
        store.put(c.getId(), c);
        return c;
    }
    public void delete(Long id) { store.remove(id); }
    public Optional<Client> findByUsername(String username) {
        return store.values().stream()
                .filter(c -> c.getUsername().equals(username))
                .findFirst();
    }
}

