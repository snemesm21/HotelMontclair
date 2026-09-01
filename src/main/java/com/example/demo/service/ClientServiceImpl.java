package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {
    private final ClientRepository repository;

    @Autowired
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
    public Client login(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || password == null) return null;
        String idTrimmed = usernameOrEmail.trim();
        String passTrimmed = password.trim();
        return repository.findByUsernameOrEmail(idTrimmed)
                .filter(c -> c.getPassword() != null && passTrimmed.equals(c.getPassword().trim()))
                .orElse(null);
    }
}
