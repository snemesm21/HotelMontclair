package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.errors.NotFoundException;
import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientRepository repository;

    @Override
    public List<Client> findAll() {
        return repository.findAll();
    }

    @Override
    public Client findById(Long id) {
        Client client = repository.findById(id).orElse(null);
        if (client == null) {
            throw new NotFoundException(id);
        }
        return client;
    }

    @Override
    @Transactional
    public Client save(Client client) {
        return repository.save(client);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Client client = repository.findById(id).orElse(null);
        if (client == null) {
            throw new NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public Client login(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || password == null) return null;
        String idTrimmed = usernameOrEmail.trim();
        String passTrimmed = password.trim();
        return repository.findByUsernameIgnoreCaseOrEmailIgnoreCase(idTrimmed, idTrimmed)
                .filter(c -> c.getPassword() != null && passTrimmed.equals(c.getPassword().trim()))
                .orElse(null);
    }
}
