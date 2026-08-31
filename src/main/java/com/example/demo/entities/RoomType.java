package com.example.demo.entities;

public class RoomType {
    private Long id;
    private String name;
    private String description;
    private double pricePerNight;

    public RoomType() {}

    public RoomType(Long id, String name, String description, double pricePerNight) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pricePerNight = pricePerNight;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }
}

