package com.gighub.auth.dto;

public class AvailabilityResponse {

    private final boolean available;

    public AvailabilityResponse(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }
}
