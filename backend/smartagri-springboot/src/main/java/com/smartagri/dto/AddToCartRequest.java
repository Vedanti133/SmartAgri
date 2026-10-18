package com.smartagri.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AddToCartRequest {

    @NotNull(message = "Fertilizer ID is required")
    private Long fertilizerId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    public AddToCartRequest() {}

    public AddToCartRequest(Long fertilizerId, Integer quantity) {
        this.fertilizerId = fertilizerId;
        this.quantity = quantity;
    }

    public Long getFertilizerId() { return fertilizerId; }
    public void setFertilizerId(Long fertilizerId) { this.fertilizerId = fertilizerId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
