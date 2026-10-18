package com.smartagri.dto;

import com.smartagri.entity.BatchLocationStock;
import com.smartagri.entity.LocationType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BatchLocationStockDTO {

    private Long id;
    private Long batchId;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;

    private Long fertilizerId;
    private String fertilizerName;
    private String sku;
    private String brand;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;

    private Long locationId;
    private String locationCode;
    private String locationName;
    private LocationType locationType;

    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BatchLocationStockDTO() {
    }

    public static BatchLocationStockDTO fromEntity(BatchLocationStock bls) {
        if (bls == null) return null;
        BatchLocationStockDTO dto = new BatchLocationStockDTO();
        dto.setId(bls.getId());
        if (bls.getBatch() != null) {
            dto.setBatchId(bls.getBatch().getId());
            dto.setBatchNumber(bls.getBatch().getBatchNumber());
            dto.setManufacturingDate(bls.getBatch().getManufacturingDate());
            dto.setExpiryDate(bls.getBatch().getExpiryDate());
            if (bls.getBatch().getFertilizer() != null) {
                dto.setFertilizerId(bls.getBatch().getFertilizer().getId());
                dto.setFertilizerName(bls.getBatch().getFertilizer().getName());
                dto.setSku(bls.getBatch().getFertilizer().getSku());
                dto.setBrand(bls.getBatch().getFertilizer().getBrand());
            }
        }
        if (bls.getWarehouse() != null) {
            dto.setWarehouseId(bls.getWarehouse().getId());
            dto.setWarehouseCode(bls.getWarehouse().getWarehouseCode());
            dto.setWarehouseName(bls.getWarehouse().getWarehouseName());
        }
        if (bls.getLocation() != null) {
            dto.setLocationId(bls.getLocation().getId());
            dto.setLocationCode(bls.getLocation().getLocationCode());
            dto.setLocationName(bls.getLocation().getName());
            dto.setLocationType(bls.getLocation().getLocationType());
        }
        dto.setQuantity(bls.getQuantity());
        dto.setReservedQuantity(bls.getReservedQuantity());
        dto.setAvailableQuantity(bls.getAvailableQuantity());
        dto.setCreatedAt(bls.getCreatedAt());
        dto.setUpdatedAt(bls.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
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

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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
}
