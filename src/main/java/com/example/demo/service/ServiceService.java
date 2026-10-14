package com.example.demo.service;

import com.example.demo.entities.Service;
import java.util.Collection;

public interface ServiceService {
    Service searchById(Long id);
    Collection<Service> searchAll();
    Collection<Service> searchByHidden(boolean hidden);
    Service save(Service service);
    Service update(Long id, Service service);
    void delete(Long id);
}