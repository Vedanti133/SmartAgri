package com.smartagri.dto;

import jakarta.validation.constraints.NotNull;

public class CreatePickListRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private Long warehouseId;
    private String assignedUser;
    private String notes;

    public CreatePickListRequest() {}

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public String getAssignedUser() { return assignedUser; }
    public void setAssignedUser(String assignedUser) { this.assignedUser = assignedUser; }

    public String getAssignedPicker() {
        return assignedUser;
    }
    public void setAssignedPicker(String assignedPicker) {
        this.assignedUser = assignedPicker;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
