package com.smartagri.dto;

import java.time.LocalDateTime;

public class FulfillmentAllocationDTO {
    private Long id;
    private String allocationNumber;
    private Long orderId;
    private String orderNumber;
    private Long orderItemId;
    private Long fertilizerId;
    private String productName;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private Long batchId;
    private String batchNumber;
    private Integer allocatedQuantity;
    private Integer pickedQuantity;
    private Integer packedQuantity;
    private String status;
    private LocalDateTime allocatedAt;
    private LocalDateTime pickedAt;
    private LocalDateTime packedAt;

    public FulfillmentAllocationDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAllocationNumber() { return allocationNumber; }
    public void setAllocationNumber(String allocationNumber) { this.allocationNumber = allocationNumber; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }

    public Long getFertilizerId() { return fertilizerId; }
    public void setFertilizerId(Long fertilizerId) { this.fertilizerId = fertilizerId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public Integer getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(Integer allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }

    public Integer getPickedQuantity() { return pickedQuantity; }
    public void setPickedQuantity(Integer pickedQuantity) { this.pickedQuantity = pickedQuantity; }

    public Integer getPackedQuantity() { return packedQuantity; }
    public void setPackedQuantity(Integer packedQuantity) { this.packedQuantity = packedQuantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(LocalDateTime allocatedAt) { this.allocatedAt = allocatedAt; }

    public LocalDateTime getPickedAt() { return pickedAt; }
    public void setPickedAt(LocalDateTime pickedAt) { this.pickedAt = pickedAt; }

    public LocalDateTime getPackedAt() { return packedAt; }
    public void setPackedAt(LocalDateTime packedAt) { this.packedAt = packedAt; }
}
