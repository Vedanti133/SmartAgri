package com.smartagri.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ShipmentDTO {
    private Long id;
    private String shipmentNumber;
    private Long orderId;
    private String orderNumber;
    private Long packageId;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long deliveryPartnerId;
    private String courierName;
    private String courierCode;
    private String trackingNumber;
    private String shippingMethod;
    private BigDecimal shippingCost;
    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String status;
    private LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;
    private String dispatchedBy;
    private String deliveryFailureReason;
    private String deliveryFailureNotes;
    private String deliveryProofReceiver;
    private String notes;
    private LocalDateTime createdAt;
    private List<ShipmentTrackingEventDTO> trackingEvents = new ArrayList<>();

    public ShipmentDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShipmentNumber() { return shipmentNumber; }
    public void setShipmentNumber(String shipmentNumber) { this.shipmentNumber = shipmentNumber; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

    public Long getDeliveryPartnerId() { return deliveryPartnerId; }
    public void setDeliveryPartnerId(Long deliveryPartnerId) { this.deliveryPartnerId = deliveryPartnerId; }

    public String getCourierName() { return courierName; }
    public void setCourierName(String courierName) { this.courierName = courierName; }

    public String getCourierCode() { return courierCode; }
    public void setCourierCode(String courierCode) { this.courierCode = courierCode; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }

    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }

    public LocalDate getActualDeliveryDate() { return actualDeliveryDate; }
    public void setActualDeliveryDate(LocalDate actualDeliveryDate) { this.actualDeliveryDate = actualDeliveryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getDispatchedBy() { return dispatchedBy; }
    public void setDispatchedBy(String dispatchedBy) { this.dispatchedBy = dispatchedBy; }

    public String getDeliveryFailureReason() { return deliveryFailureReason; }
    public void setDeliveryFailureReason(String deliveryFailureReason) { this.deliveryFailureReason = deliveryFailureReason; }

    public String getDeliveryFailureNotes() { return deliveryFailureNotes; }
    public void setDeliveryFailureNotes(String deliveryFailureNotes) { this.deliveryFailureNotes = deliveryFailureNotes; }

    public String getDeliveryProofReceiver() { return deliveryProofReceiver; }
    public void setDeliveryProofReceiver(String deliveryProofReceiver) { this.deliveryProofReceiver = deliveryProofReceiver; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<ShipmentTrackingEventDTO> getTrackingEvents() { return trackingEvents; }
    public void setTrackingEvents(List<ShipmentTrackingEventDTO> trackingEvents) { this.trackingEvents = trackingEvents; }

    public List<ShipmentTrackingEventDTO> getEvents() { return trackingEvents; }
    public void setEvents(List<ShipmentTrackingEventDTO> events) { this.trackingEvents = events; }

    public String getReceivedBy() { return deliveryProofReceiver; }
    public void setReceivedBy(String receivedBy) { this.deliveryProofReceiver = receivedBy; }

    public String getLocation() {
        if (trackingEvents != null && !trackingEvents.isEmpty()) {
            return trackingEvents.get(trackingEvents.size() - 1).getLocation();
        }
        return warehouseName;
    }
    public void setLocation(String location) {}
}
