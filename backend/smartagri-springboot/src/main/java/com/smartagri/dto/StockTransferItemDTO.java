package com.smartagri.dto;

import com.smartagri.entity.StockTransferItem;

import java.time.LocalDateTime;

public class StockTransferItemDTO {

    private Long id;
    private Long fertilizerId;
    private String fertilizerName;
    private String sku;
    private Long batchId;
    private String batchNumber;
    private Long sourceLocationId;
    private String sourceLocationCode;
    private String sourceLocationName;
    private Long destinationLocationId;
    private String destinationLocationCode;
    private String destinationLocationName;
    private Integer requestedQuantity;
    private Integer dispatchedQuantity;
    private Integer receivedQuantity;
    private Integer remainingToReceive;
    private String notes;
    private LocalDateTime createdAt;

    public StockTransferItemDTO() {
    }

    public static StockTransferItemDTO fromEntity(StockTransferItem item) {
        if (item == null) return null;
        StockTransferItemDTO dto = new StockTransferItemDTO();
        dto.setId(item.getId());
        if (item.getFertilizer() != null) {
            dto.setFertilizerId(item.getFertilizer().getId());
            dto.setFertilizerName(item.getFertilizer().getName());
            dto.setSku(item.getFertilizer().getSku());
        }
        if (item.getBatch() != null) {
            dto.setBatchId(item.getBatch().getId());
            dto.setBatchNumber(item.getBatch().getBatchNumber());
        }
        if (item.getSourceLocation() != null) {
            dto.setSourceLocationId(item.getSourceLocation().getId());
            dto.setSourceLocationCode(item.getSourceLocation().getLocationCode());
            dto.setSourceLocationName(item.getSourceLocation().getName());
        }
        if (item.getDestinationLocation() != null) {
            dto.setDestinationLocationId(item.getDestinationLocation().getId());
            dto.setDestinationLocationCode(item.getDestinationLocation().getLocationCode());
            dto.setDestinationLocationName(item.getDestinationLocation().getName());
        }
        dto.setRequestedQuantity(item.getRequestedQuantity());
        dto.setDispatchedQuantity(item.getDispatchedQuantity());
        dto.setReceivedQuantity(item.getReceivedQuantity());
        dto.setRemainingToReceive(item.getRemainingToReceive());
        dto.setNotes(item.getNotes());
        dto.setCreatedAt(item.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFertilizerId() {
        return fertilizerId;
    }

    public void setFertilizerId(Long fertilizerId) {
        this.fertilizerId = fertilizerId;
    }

    public String getFertilizerName() {
        return fertilizerName;
    }

    public void setFertilizerName(String fertilizerName) {
        this.fertilizerName = fertilizerName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public String getSourceLocationCode() {
        return sourceLocationCode;
    }

    public void setSourceLocationCode(String sourceLocationCode) {
        this.sourceLocationCode = sourceLocationCode;
    }

    public String getSourceLocationName() {
        return sourceLocationName;
    }

    public void setSourceLocationName(String sourceLocationName) {
        this.sourceLocationName = sourceLocationName;
    }

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(Long destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public String getDestinationLocationCode() {
        return destinationLocationCode;
    }

    public void setDestinationLocationCode(String destinationLocationCode) {
        this.destinationLocationCode = destinationLocationCode;
    }

    public String getDestinationLocationName() {
        return destinationLocationName;
    }

    public void setDestinationLocationName(String destinationLocationName) {
        this.destinationLocationName = destinationLocationName;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Integer getDispatchedQuantity() {
        return dispatchedQuantity;
    }

    public void setDispatchedQuantity(Integer dispatchedQuantity) {
        this.dispatchedQuantity = dispatchedQuantity;
    }

    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public Integer getRemainingToReceive() {
        return remainingToReceive;
    }

    public void setRemainingToReceive(Integer remainingToReceive) {
        this.remainingToReceive = remainingToReceive;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
