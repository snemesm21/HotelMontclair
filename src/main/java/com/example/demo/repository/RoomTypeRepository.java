package com.example.demo.repository;

import com.example.demo.entities.RoomType;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RoomTypeRepository {
    private final Map<Long, RoomType> store = new LinkedHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @PostConstruct
    public void init() {
        save(new RoomType(null, "Simple", "Habitación estándar", 80.0));
        save(new RoomType(null, "Suite", "Habitación de lujo", 200.0));
    }

    public List<RoomType> findAll() { return new ArrayList<>(store.values()); }
    public RoomType findById(Long id) { return store.get(id); }
    public RoomType save(RoomType rt) {
        if (rt.getId() == null) rt.setId(idGen.getAndIncrement());
        store.put(rt.getId(), rt);
        return rt;
    }
    public void delete(Long id) { store.remove(id); }
}

