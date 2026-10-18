package com.smartagri.dto;

import jakarta.validation.constraints.NotNull;

public class AddToWishlistRequest {

    @NotNull(message = "Fertilizer ID is required")
    private Long fertilizerId;

    public AddToWishlistRequest() {}

    public AddToWishlistRequest(Long fertilizerId) {
        this.fertilizerId = fertilizerId;
    }

    public Long getFertilizerId() { return fertilizerId; }
    public void setFertilizerId(Long fertilizerId) { this.fertilizerId = fertilizerId; }
}
