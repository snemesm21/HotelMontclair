package com.example.demo.service;

import com.example.demo.entities.Service;
import java.util.Collection;

public interface ServiceService {
    Service searchById(Long id);
    Collection<Service> searchAll();
    void save(Service service);
    void delete(Long id);
}