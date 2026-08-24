package com.example.demo.service;

import com.example.demo.entities.Service;
import com.example.demo.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

@org.springframework.stereotype.Service
public class ServicesServiceImpl implements ServiceService {

    @Autowired
    private ServiceRepository repo;

    @Override
    public Service searchById(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public Collection<Service> searchAll() {
        return repo.findAll();
    }

    @Override
    public void save(Service service) {
        repo.save(service);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}