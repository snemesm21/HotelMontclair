package com.example.demo.service;

public class TourismRoutingException extends RuntimeException {
    public TourismRoutingException(String message) {
        super(message);
    }

    public TourismRoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}
