package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.repository.ClientRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {
    private final ClientRepository repository;

    public ClientServiceImpl(ClientRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Client> findAll() {
        return repository.findAll();
    }

    @Override
    public Client findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Client save(Client client) {
        return repository.save(client);
    }

    @Override
    public void delete(Long id) {
        repository.delete(id);
    }

    @Override
    public Client login(String username, String password) {
        return repository.findByUsername(username)
                .filter(c -> c.getPassword().equals(password))
                .orElse(null);
    }
}

