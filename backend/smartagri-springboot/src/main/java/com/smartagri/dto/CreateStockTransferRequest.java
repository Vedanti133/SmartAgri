package com.smartagri.dto;

import com.smartagri.entity.StockTransferStatus;

import java.util.ArrayList;
import java.util.List;

public class CreateStockTransferRequest {

    private Long sourceWarehouseId;
    private Long destinationWarehouseId;
    private String notes;
    private String reason;
    private StockTransferStatus status = StockTransferStatus.DRAFT;
    private List<CreateStockTransferItemRequest> items = new ArrayList<>();

    public CreateStockTransferRequest() {
    }

    public Long getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public void setSourceWarehouseId(Long sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    public void setDestinationWarehouseId(Long destinationWarehouseId) {
        this.destinationWarehouseId = destinationWarehouseId;
    }

    public String getNotes() {
        return notes != null ? notes : reason;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        if (this.reason == null) this.reason = notes;
    }

    public String getReason() {
        return reason != null ? reason : notes;
    }

    public void setReason(String reason) {
        this.reason = reason;
        if (this.notes == null) this.notes = reason;
    }

    public StockTransferStatus getStatus() {
        return status;
    }

    public void setStatus(StockTransferStatus status) {
        this.status = status;
    }

    public List<CreateStockTransferItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreateStockTransferItemRequest> items) {
        this.items = items;
    }
}
