package com.smartagri.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FulfillmentPackageDTO {
    private Long id;
    private String packageNumber;
    private Long orderId;
    private String orderNumber;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String packageStatus;
    private BigDecimal actualWeight;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal volumetricWeight;
    private BigDecimal chargeableWeight;
    private String packagingType;
    private String packedBy;
    private LocalDateTime packedAt;
    private String trackingNumber;
    private String courier;
    private String notes;
    private LocalDateTime createdAt;
    private List<FulfillmentPackageItemDTO> items = new ArrayList<>();

    public FulfillmentPackageDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPackageNumber() { return packageNumber; }
    public void setPackageNumber(String packageNumber) { this.packageNumber = packageNumber; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

    public String getPackageStatus() { return packageStatus; }
    public void setPackageStatus(String packageStatus) { this.packageStatus = packageStatus; }

    public BigDecimal getActualWeight() { return actualWeight; }
    public void setActualWeight(BigDecimal actualWeight) { this.actualWeight = actualWeight; }

    public BigDecimal getLengthCm() { return lengthCm; }
    public void setLengthCm(BigDecimal lengthCm) { this.lengthCm = lengthCm; }

    public BigDecimal getWidthCm() { return widthCm; }
    public void setWidthCm(BigDecimal widthCm) { this.widthCm = widthCm; }

    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }

    public BigDecimal getVolumetricWeight() { return volumetricWeight; }
    public void setVolumetricWeight(BigDecimal volumetricWeight) { this.volumetricWeight = volumetricWeight; }

    public BigDecimal getChargeableWeight() { return chargeableWeight; }
    public void setChargeableWeight(BigDecimal chargeableWeight) { this.chargeableWeight = chargeableWeight; }

    public String getPackagingType() { return packagingType; }
    public void setPackagingType(String packagingType) { this.packagingType = packagingType; }

    public String getPackedBy() { return packedBy; }
    public void setPackedBy(String packedBy) { this.packedBy = packedBy; }

    public LocalDateTime getPackedAt() { return packedAt; }
    public void setPackedAt(LocalDateTime packedAt) { this.packedAt = packedAt; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getCourier() { return courier; }
    public void setCourier(String courier) { this.courier = courier; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<FulfillmentPackageItemDTO> getItems() { return items; }
    public void setItems(List<FulfillmentPackageItemDTO> items) { this.items = items; }

    public String getStatus() { return packageStatus; }
    public void setStatus(String status) { this.packageStatus = status; }

    public BigDecimal getActualWeightKg() { return actualWeight; }
    public void setActualWeightKg(BigDecimal actualWeightKg) { this.actualWeight = actualWeightKg; }

    public BigDecimal getVolumetricWeightKg() { return volumetricWeight; }
    public void setVolumetricWeightKg(BigDecimal volumetricWeightKg) { this.volumetricWeight = volumetricWeightKg; }

    public BigDecimal getChargeableWeightKg() { return chargeableWeight; }
    public void setChargeableWeightKg(BigDecimal chargeableWeightKg) { this.chargeableWeight = chargeableWeightKg; }
}
