package com.smartagri.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ApproveReturnRequest {

    private String adminNotes;
    private String carrier;
    private LocalDateTime pickupScheduledAt;
    private Long destinationWarehouseId;
    // Optional map of itemId -> approvedQuantity. If null/empty, requested quantities are approved.
    private Map<Long, Integer> approvedQuantities;

    public ApproveReturnRequest() {}

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

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

    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    public void setDestinationWarehouseId(Long destinationWarehouseId) {
        this.destinationWarehouseId = destinationWarehouseId;
    }

    public Map<Long, Integer> getApprovedQuantities() {
        return approvedQuantities;
    }

    public void setApprovedQuantities(Map<Long, Integer> approvedQuantities) {
        this.approvedQuantities = approvedQuantities;
    }
}
