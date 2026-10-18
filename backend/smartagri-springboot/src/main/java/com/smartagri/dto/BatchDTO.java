package com.smartagri.dto;

import com.smartagri.entity.BatchStatus;
import com.smartagri.entity.FertilizerBatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class BatchDTO {

    private Long id;
    private Long fertilizerId;
    private String fertilizerName;
    private String sku;
    private String brand;
    private String categoryName;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private Integer initialQuantity;
    private Integer currentQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private String supplierName;
    private String supplierReference;
    private String purchaseOrderNumber;
    private BatchStatus status;
    private String statusDisplay;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean expired;
    private boolean nearExpiry;
    private Long daysUntilExpiry;

    public BatchDTO() {
    }

    public static BatchDTO fromEntity(FertilizerBatch entity) {
        if (entity == null) {
            return null;
        }
        BatchDTO dto = new BatchDTO();
        dto.setId(entity.getId());
        if (entity.getFertilizer() != null) {
            dto.setFertilizerId(entity.getFertilizer().getId());
            dto.setFertilizerName(entity.getFertilizer().getName());
            dto.setSku(entity.getFertilizer().getSku());
            dto.setBrand(entity.getFertilizer().getBrand());
            if (entity.getFertilizer().getCategory() != null) {
                dto.setCategoryName(entity.getFertilizer().getCategory().getName());
            }
        }
        dto.setBatchNumber(entity.getBatchNumber());
        dto.setManufacturingDate(entity.getManufacturingDate());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setInitialQuantity(entity.getInitialQuantity());
        dto.setCurrentQuantity(entity.getCurrentQuantity());
        dto.setReservedQuantity(entity.getReservedQuantity());
        dto.setAvailableQuantity(entity.getAvailableQuantity());
        dto.setSupplierName(entity.getSupplierName());
        dto.setSupplierReference(entity.getSupplierReference());
        dto.setPurchaseOrderNumber(entity.getPurchaseOrderNumber());
        
        BatchStatus dynamicStatus = entity.computeDynamicStatus();
        dto.setStatus(dynamicStatus);
        dto.setStatusDisplay(dynamicStatus != null ? dynamicStatus.name() : "UNKNOWN");

        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setExpired(entity.isExpired());
        dto.setNearExpiry(entity.isNearExpiry());

        if (entity.getExpiryDate() != null) {
            dto.setDaysUntilExpiry(ChronoUnit.DAYS.between(LocalDate.now(), entity.getExpiryDate()));
        }

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

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public void setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(Integer initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public Integer getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(Integer currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierReference() {
        return supplierReference;
    }

    public void setSupplierReference(String supplierReference) {
        this.supplierReference = supplierReference;
    }

    public String getPurchaseOrderNumber() {
        return purchaseOrderNumber;
    }

    public void setPurchaseOrderNumber(String purchaseOrderNumber) {
        this.purchaseOrderNumber = purchaseOrderNumber;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public String getStatusDisplay() {
        return statusDisplay;
    }

    public void setStatusDisplay(String statusDisplay) {
        this.statusDisplay = statusDisplay;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public boolean isNearExpiry() {
        return nearExpiry;
    }

    public void setNearExpiry(boolean nearExpiry) {
        this.nearExpiry = nearExpiry;
    }

    public Long getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    public void setDaysUntilExpiry(Long daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
    }
}
