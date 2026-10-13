package com.example.demo.service;

import com.example.demo.entities.Service;
import com.example.demo.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@org.springframework.stereotype.Service
public class ServicesServiceImpl implements ServiceService {

    @Autowired
    private ServiceRepository repo;

    @Override
    public Service searchById(Long id) {
        if (id == null) return null;
        return repo.findById(id).orElse(null);
    }

    @Override
    public Collection<Service> searchAll() {
        return repo.findAll();
    }

    @Override
    public Collection<Service> searchByHidden(boolean hidden) {
        return repo.findByHidden(hidden);
    }

    @Override
    @Transactional
    public Service save(Service service) {
        return repo.save(service);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id != null && repo.existsById(id)) {
            repo.deleteById(id);
        }
    }
}