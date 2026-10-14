package com.example.demo.controller;

import com.example.demo.entities.Service;
import com.example.demo.service.ServiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServicesControllerTest {

    private ServicesController controller;
    private Service hiddenService;
    private Service visibleService;

    @BeforeEach
    void setUp() {
        controller = new ServicesController();

        hiddenService = Service.builder()
                .id(1L)
                .name("Servicio Oculto")
                .description("Solo para administradores")
                .price(50.0)
                .hidden(true)
                .build();

        visibleService = Service.builder()
                .id(2L)
                .name("Servicio Visible")
                .description("Para todos los clientes")
                .price(100.0)
                .hidden(false)
                .build();

        ServiceService mockService = new ServiceService() {
            @Override
            public Service searchById(Long id) {
                if (Long.valueOf(1L).equals(id)) return hiddenService;
                if (Long.valueOf(2L).equals(id)) return visibleService;
                return null;
            }

            @Override
            public Collection<Service> searchAll() {
                return List.of(hiddenService, visibleService);
            }

            @Override
            public Collection<Service> searchByHidden(boolean hidden) {
                if (hidden) {
                    return List.of(hiddenService);
                }
                return List.of(visibleService);
            }

            @Override
            public Service save(Service service) {
                return service;
            }

            @Override
            public Service update(Long id, Service service) {
                return service;
            }

            @Override
            public void delete(Long id) {
            }
        };

        ReflectionTestUtils.setField(controller, "serviceService", mockService);
    }

    @Test
    void testGetNotHiddenServicesEndpoint() {
        Collection<Service> result = controller.getNotHiddenServices();

        assertNotNull(result);
        assertEquals(1, result.size());
        Service s = result.iterator().next();
        assertEquals(2L, s.getId());
        assertEquals(false, s.isHidden());
    }

    @Test
    void testGetAllServicesWithHiddenParam() {
        Collection<Service> hiddenResult = controller.getAllServices(true);
        assertEquals(1, hiddenResult.size());
        assertEquals(true, hiddenResult.iterator().next().isHidden());

        Collection<Service> visibleResult = controller.getAllServices(false);
        assertEquals(1, visibleResult.size());
        assertEquals(false, visibleResult.iterator().next().isHidden());

        Collection<Service> allResult = controller.getAllServices(null);
        assertEquals(2, allResult.size());
    }
}
