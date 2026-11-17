package com.example.demo.mapper;

import com.example.demo.dto.RoomTypeDTO;
import com.example.demo.entities.RoomType;

/**
 * Mapper del patrón enseñado en clase para convertir la entidad RoomType en su RoomTypeDTO plano.
 */
public interface RoomTypeMapper {

    RoomTypeMapper INSTANCE = RoomTypeDTO::fromEntity;

    RoomTypeDTO convert(RoomType roomType);
}
