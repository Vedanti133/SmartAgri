package com.smartagri.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PickListDTO {
    private Long id;
    private String pickListNumber;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long orderId;
    private String orderNumber;
    private String assignedUser;
    private String status;
    private String notes;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private List<PickListItemDTO> items = new ArrayList<>();

    public PickListDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPickListNumber() { return pickListNumber; }
    public void setPickListNumber(String pickListNumber) { this.pickListNumber = pickListNumber; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getAssignedUser() { return assignedUser; }
    public void setAssignedUser(String assignedUser) { this.assignedUser = assignedUser; }

    public String getAssignedPicker() { return assignedUser; }
    public void setAssignedPicker(String assignedPicker) { this.assignedUser = assignedPicker; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<PickListItemDTO> getItems() { return items; }
    public void setItems(List<PickListItemDTO> items) { this.items = items; }
}
