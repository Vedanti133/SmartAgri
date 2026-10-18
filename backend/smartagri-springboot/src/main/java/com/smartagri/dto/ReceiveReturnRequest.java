package com.smartagri.dto;

import java.util.Map;

public class ReceiveReturnRequest {

    private Long warehouseId;
    private String notes;
    private Map<Long, Integer> receivedQuantities;

    public ReceiveReturnRequest() {}

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Map<Long, Integer> getReceivedQuantities() {
        return receivedQuantities;
    }

    public void setReceivedQuantities(Map<Long, Integer> receivedQuantities) {
        this.receivedQuantities = receivedQuantities;
    }
}
