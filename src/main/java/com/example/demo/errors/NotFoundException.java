package com.example.demo.errors;

public class NotFoundException extends RuntimeException {
    public NotFoundException(Long id) {
        super("No se pudo encontrar el registro con ID " + id);
    }
}
