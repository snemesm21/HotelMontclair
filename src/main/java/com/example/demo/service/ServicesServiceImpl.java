package com.example.demo.service;

import com.example.demo.entities.Service;
import com.example.demo.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.errors.NotFoundException;

import java.util.Collection;

@org.springframework.stereotype.Service
public class ServicesServiceImpl implements ServiceService {

    @Autowired
    private ServiceRepository repo;

    @Override
    public Service searchById(Long id) {
        Service service = repo.findById(id).orElse(null);
        if (service == null) {
            throw new NotFoundException(id);
        }
        return service;
    }

    @Override
    public Collection<Service> searchAll() {
        return repo.findAll();
    }

    @Override
    @Transactional
    public void save(Service service) {
        repo.save(service);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Service service = repo.findById(id).orElse(null);
        if (service == null) {
            throw new NotFoundException(id);
        }
        repo.deleteById(id);
    }
}