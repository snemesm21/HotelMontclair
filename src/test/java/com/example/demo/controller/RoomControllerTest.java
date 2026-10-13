package com.example.demo.controller;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomType;
import com.example.demo.service.RoomService;
import com.example.demo.service.RoomTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RoomControllerTest {

    private RoomController controller;
    private Room roomType1;
    private Room roomType2;

    @BeforeEach
    void setUp() {
        controller = new RoomController();

        RoomType type1 = RoomType.builder().id(1L).name("Deluxe").build();
        RoomType type2 = RoomType.builder().id(2L).name("Suite").build();

        roomType1 = Room.builder().id(101L).number("101").type(type1).build();
        roomType2 = Room.builder().id(201L).number("201").type(type2).build();

        RoomService mockRoomService = new RoomService() {
            @Override
            public List<Room> findAll() {
                return List.of(roomType1, roomType2);
            }

            @Override
            public Room findById(Long id) {
                if (Long.valueOf(101L).equals(id)) return roomType1;
                if (Long.valueOf(201L).equals(id)) return roomType2;
                return null;
            }

            @Override
            public List<Room> findByTypeId(Long typeId) {
                if (Long.valueOf(1L).equals(typeId)) {
                    return List.of(roomType1);
                }
                if (Long.valueOf(2L).equals(typeId)) {
                    return List.of(roomType2);
                }
                return List.of();
            }

            @Override
            public Room save(Room room) {
                return room;
            }

            @Override
            public void delete(Long id) {
            }
        };

        ReflectionTestUtils.setField(controller, "roomService", mockRoomService);
    }

    @Test
    void testGetRoomsByTypeId() {
        List<Room> result = controller.getRoomsByTypeId(1L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("101", result.get(0).getNumber());
    }

    @Test
    void testGetAllRoomsWithTypeIdParam() {
        List<Room> filtered = controller.getAllRooms(2L);
        assertEquals(1, filtered.size());
        assertEquals("201", filtered.get(0).getNumber());

        List<Room> all = controller.getAllRooms(null);
        assertEquals(2, all.size());
    }
}
