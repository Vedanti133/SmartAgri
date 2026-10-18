package com.smartagri.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateDeliveryStatusRequest {

    @NotBlank(message = "Delivery status is required")
    private String deliveryStatus;

    private String notes;

    public UpdateDeliveryStatusRequest() {}

    public UpdateDeliveryStatusRequest(String deliveryStatus, String notes) {
        this.deliveryStatus = deliveryStatus;
        this.notes = notes;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
