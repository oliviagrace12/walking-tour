package com.example.walkingtours.domain;

public class FenceData {
    private String id;
    private double latitude;
    private double longitude;
    private float radius;
    private String fenceColor;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public String getFenceColor() {
        return fenceColor;
    }

    public void setFenceColor(String fenceColor) {
        this.fenceColor = fenceColor;
    }

}
