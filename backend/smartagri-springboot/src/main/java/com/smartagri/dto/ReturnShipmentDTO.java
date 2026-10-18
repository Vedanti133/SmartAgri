package com.smartagri.dto;

import com.smartagri.enums.ReturnShipmentStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReturnShipmentDTO {

    private Long id;
    private Long returnRequestId;
    private String shipmentNumber;
    private String trackingNumber;
    private String carrier;
    private String pickupAddress;
    private Long destinationWarehouseId;
    private String destinationWarehouseName;
    private ReturnShipmentStatus status;
    private LocalDateTime pickupScheduledAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime receivedAt;
    private String notes;
    private List<ReturnTrackingEventDTO> trackingEvents = new ArrayList<>();

    public ReturnShipmentDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReturnRequestId() {
        return returnRequestId;
    }

    public void setReturnRequestId(Long returnRequestId) {
        this.returnRequestId = returnRequestId;
    }

    public String getShipmentNumber() {
        return shipmentNumber;
    }

    public void setShipmentNumber(String shipmentNumber) {
        this.shipmentNumber = shipmentNumber;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    public void setDestinationWarehouseId(Long destinationWarehouseId) {
        this.destinationWarehouseId = destinationWarehouseId;
    }

    public String getDestinationWarehouseName() {
        return destinationWarehouseName;
    }

    public void setDestinationWarehouseName(String destinationWarehouseName) {
        this.destinationWarehouseName = destinationWarehouseName;
    }

    public ReturnShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ReturnShipmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getPickupScheduledAt() {
        return pickupScheduledAt;
    }

    public void setPickupScheduledAt(LocalDateTime pickupScheduledAt) {
        this.pickupScheduledAt = pickupScheduledAt;
    }

    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(LocalDateTime pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ReturnTrackingEventDTO> getTrackingEvents() {
        return trackingEvents;
    }

    public void setTrackingEvents(List<ReturnTrackingEventDTO> trackingEvents) {
        this.trackingEvents = trackingEvents;
    }
}
