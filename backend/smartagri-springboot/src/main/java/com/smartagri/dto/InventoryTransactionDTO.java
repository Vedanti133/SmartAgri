package com.smartagri.dto;

import com.smartagri.entity.InventoryTransaction;
import com.smartagri.entity.InventoryTransactionType;

import java.time.LocalDateTime;

public class InventoryTransactionDTO {

    private Long id;
    private Long fertilizerId;
    private String productName;
    private String sku;
    private String brand;
    private InventoryTransactionType transactionType;
    private String transactionTypeDisplay;
    private Integer quantity;
    private Integer stockBefore;
    private Integer stockAfter;
    private String referenceType;
    private String referenceId;
    private String reason;
    private String performedBy;
    private LocalDateTime createdAt;
    private Long batchId;
    private String batchNumber;
    private java.time.LocalDate expiryDate;
    private Long warehouseId;
    private String warehouseCode;
    private Long locationId;
    private String locationCode;

    public InventoryTransactionDTO() {
    }

    public static InventoryTransactionDTO fromEntity(InventoryTransaction tx) {
        if (tx == null) return null;
        InventoryTransactionDTO dto = new InventoryTransactionDTO();
        dto.setId(tx.getId());
        if (tx.getFertilizer() != null) {
            dto.setFertilizerId(tx.getFertilizer().getId());
            dto.setProductName(tx.getFertilizer().getName());
            dto.setSku(tx.getFertilizer().getSku());
            dto.setBrand(tx.getFertilizer().getBrand());
        }
        if (tx.getBatch() != null) {
            dto.setBatchId(tx.getBatch().getId());
            dto.setBatchNumber(tx.getBatch().getBatchNumber());
            dto.setExpiryDate(tx.getBatch().getExpiryDate());
        }
        if (tx.getWarehouse() != null) {
            dto.setWarehouseId(tx.getWarehouse().getId());
            dto.setWarehouseCode(tx.getWarehouse().getWarehouseCode());
        }
        if (tx.getLocation() != null) {
            dto.setLocationId(tx.getLocation().getId());
            dto.setLocationCode(tx.getLocation().getLocationCode());
        }
        dto.setTransactionType(tx.getTransactionType());
        dto.setTransactionTypeDisplay(tx.getTransactionType() != null ? tx.getTransactionType().name() : "");
        dto.setQuantity(tx.getQuantity());
        dto.setStockBefore(tx.getStockBefore());
        dto.setStockAfter(tx.getStockAfter());
        dto.setReferenceType(tx.getReferenceType());
        dto.setReferenceId(tx.getReferenceId());
        dto.setReason(tx.getReason());
        dto.setPerformedBy(tx.getPerformedBy());
        dto.setCreatedAt(tx.getCreatedAt());
        return dto;
    }

    // Getters and Setters

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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public InventoryTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(InventoryTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getTransactionTypeDisplay() {
        return transactionTypeDisplay;
    }

    public void setTransactionTypeDisplay(String transactionTypeDisplay) {
        this.transactionTypeDisplay = transactionTypeDisplay;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getStockBefore() {
        return stockBefore;
    }

    public void setStockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
    }

    public Integer getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public java.time.LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(java.time.LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }
}
