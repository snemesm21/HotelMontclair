package com.example.demo.dto;

import com.example.demo.entities.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO plano de tipo de habitación sin la lista de habitaciones asociadas.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeDTO {

    private Long id;
    private String name;
    private String description;
    private double pricePerNight;
    private String imageUrl;

    public static RoomTypeDTO fromEntity(RoomType roomType) {
        if (roomType == null) {
            return null;
        }
        return RoomTypeDTO.builder()
                .id(roomType.getId())
                .name(roomType.getName())
                .description(roomType.getDescription())
                .pricePerNight(roomType.getPricePerNight())
                .imageUrl(roomType.getImageUrl())
                .build();
    }
}
