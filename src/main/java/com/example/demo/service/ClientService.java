package com.example.demo.service;

import com.example.demo.entities.Client;
import java.util.List;

public interface ClientService {
    List<Client> findAll();
    Client findById(Long id);
    Client save(Client client);
    void delete(Long id);
    Client login(String username, String password); // returns client if ok, else null
}

