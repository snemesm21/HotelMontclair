package com.example.demo.mapper;

import com.example.demo.dto.ClientDTO;
import com.example.demo.entities.Client;

/**
 * Mapper del patrón enseñado en clase para convertir la entidad Client en su ClientDTO seguro.
 */
public interface ClientMapper {

    ClientMapper INSTANCE = ClientDTO::fromEntity;

    ClientDTO convert(Client client);
}
