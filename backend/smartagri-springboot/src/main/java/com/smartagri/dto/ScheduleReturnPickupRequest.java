package com.smartagri.dto;

import java.time.LocalDateTime;

public class ScheduleReturnPickupRequest {

    private String carrier;
    private LocalDateTime pickupScheduledAt;
    private String trackingNumber;
    private Long destinationWarehouseId;
    private String pickupAddress;

    public ScheduleReturnPickupRequest() {}

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public LocalDateTime getPickupScheduledAt() {
        return pickupScheduledAt;
    }

    public void setPickupScheduledAt(LocalDateTime pickupScheduledAt) {
        this.pickupScheduledAt = pickupScheduledAt;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    public void setDestinationWarehouseId(Long destinationWarehouseId) {
        this.destinationWarehouseId = destinationWarehouseId;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }
}
