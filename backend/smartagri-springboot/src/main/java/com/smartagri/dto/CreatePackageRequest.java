package com.smartagri.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class CreatePackageRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private Long warehouseId;
    private BigDecimal actualWeight = BigDecimal.ZERO;
    private BigDecimal lengthCm = BigDecimal.ZERO;
    private BigDecimal widthCm = BigDecimal.ZERO;
    private BigDecimal heightCm = BigDecimal.ZERO;
    private String packagingType = "BOX";
    private String notes;
    private List<PackageItemInput> items;

    public CreatePackageRequest() {}

    public static class PackageItemInput {
        private Long orderItemId;
        private Long batchId;
        private Long warehouseId;
        private Long locationId;
        private Long allocationId;
        private Integer quantity;

        public PackageItemInput() {}

        public Long getOrderItemId() { return orderItemId; }
        public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }

        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }

        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

        public Long getLocationId() { return locationId; }
        public void setLocationId(Long locationId) { this.locationId = locationId; }

        public Long getAllocationId() { return allocationId; }
        public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public BigDecimal getActualWeight() { return actualWeight; }
    public void setActualWeight(BigDecimal actualWeight) { this.actualWeight = actualWeight; }

    public BigDecimal getLengthCm() { return lengthCm; }
    public void setLengthCm(BigDecimal lengthCm) { this.lengthCm = lengthCm; }

    public BigDecimal getWidthCm() { return widthCm; }
    public void setWidthCm(BigDecimal widthCm) { this.widthCm = widthCm; }

    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }

    public String getPackagingType() { return packagingType; }
    public void setPackagingType(String packagingType) { this.packagingType = packagingType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<PackageItemInput> getItems() { return items; }
    public void setItems(List<PackageItemInput> items) { this.items = items; }

    public BigDecimal getActualWeightKg() { return actualWeight; }
    public void setActualWeightKg(BigDecimal actualWeightKg) { this.actualWeight = actualWeightKg; }

    public String getPackageType() { return packagingType; }
    public void setPackageType(String packageType) { this.packagingType = packageType; }
}
