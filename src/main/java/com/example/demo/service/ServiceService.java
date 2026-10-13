package com.example.demo.service;

import com.example.demo.entities.Service;
import java.util.Collection;
import java.util.Collections;

public interface ServiceService {
    Service searchById(Long id);
    Collection<Service> searchAll();
    default Collection<Service> searchByHidden(boolean hidden) {
        return Collections.emptyList();
    }
    Service save(Service service);
    void delete(Long id);
}