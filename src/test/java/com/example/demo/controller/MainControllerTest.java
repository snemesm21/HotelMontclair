package com.example.demo.controller;

import com.example.demo.entities.RoomType;
import com.example.demo.service.RoomService;
import com.example.demo.service.RoomTypeService;
import com.example.demo.service.ServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MainControllerTest {

    @Test
    void landingPageLoadsPublicDataFromBackend() {
        ServiceService serviceService = new ServiceService() {
            @Override
            public com.example.demo.entities.Service searchById(Long id) {
                return null;
            }

            @Override
            public java.util.Collection<com.example.demo.entities.Service> searchAll() {
                return List.of();
            }

            @Override
            public java.util.Collection<com.example.demo.entities.Service> searchByHidden(boolean hidden) {
                return List.of();
            }

            @Override
            public com.example.demo.entities.Service save(com.example.demo.entities.Service service) {
                return service;
            }

            @Override
            public void delete(Long id) {
            }
        };
        RoomTypeService roomTypeService = new RoomTypeService() {
            @Override
            public List<RoomType> findAll() {
                return List.of(RoomType.builder()
                        .id(1L)
                        .name("Deluxe")
                        .description("Habitación deluxe")
                        .pricePerNight(120.0)
                        .imageUrl("https://example.com/deluxe.jpg")
                        .build());
            }

            @Override
            public RoomType findById(Long id) {
                return null;
            }

            @Override
            public RoomType save(RoomType roomType) {
                return roomType;
            }

            @Override
            public void delete(Long id) {
            }
        };

        MainController controller = new MainController();
        controller.setServiceService(serviceService);
        controller.setRoomTypeService(roomTypeService);

        Model model = new ExtendedModelMap();
        String viewName = controller.index(model);

        assertEquals("index", viewName);
        assertNotNull(model.getAttribute("services"));
        assertNotNull(model.getAttribute("roomTypes"));
    }

    @Test
    void roomCardsPageLoadsRoomTypesFromBackend() {
        RoomTypeService roomTypeService = new RoomTypeService() {
            @Override
            public List<RoomType> findAll() {
                return List.of(RoomType.builder()
                        .id(1L)
                        .name("Deluxe")
                        .description("Habitación deluxe")
                        .pricePerNight(120.0)
                        .imageUrl("https://example.com/deluxe.jpg")
                        .build());
            }

            @Override
            public RoomType findById(Long id) {
                return null;
            }

            @Override
            public RoomType save(RoomType roomType) {
                return roomType;
            }

            @Override
            public void delete(Long id) {
            }
        };

        MainController controller = new MainController();
        controller.setRoomTypeService(roomTypeService);

        Model model = new ExtendedModelMap();
        String viewName = controller.roomsCards(model);

        assertEquals("rooms-cards", viewName);
        assertNotNull(model.getAttribute("roomTypes"));
    }
}
