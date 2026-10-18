package com.smartagri.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CustomerTrackingDTO {
    private Long orderId;
    private String orderNumber;
    private LocalDateTime orderDate;
    private String orderStatus;
    private String fulfillmentStatus;
    private String shipmentNumber;
    private String trackingNumber;
    private String courier;
    private String trackingUrl;
    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private String currentStatus;
    private String productsSummary;
    private List<ShipmentTrackingEventDTO> trackingTimeline = new ArrayList<>();
    private List<FulfillmentPackageDTO> packages = new ArrayList<>();

    public CustomerTrackingDTO() {}

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getFulfillmentStatus() { return fulfillmentStatus; }
    public void setFulfillmentStatus(String fulfillmentStatus) { this.fulfillmentStatus = fulfillmentStatus; }

    public String getShipmentNumber() { return shipmentNumber; }
    public void setShipmentNumber(String shipmentNumber) { this.shipmentNumber = shipmentNumber; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getCourier() { return courier; }
    public void setCourier(String courier) { this.courier = courier; }

    public String getTrackingUrl() { return trackingUrl; }
    public void setTrackingUrl(String trackingUrl) { this.trackingUrl = trackingUrl; }

    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }

    public LocalDate getActualDeliveryDate() { return actualDeliveryDate; }
    public void setActualDeliveryDate(LocalDate actualDeliveryDate) { this.actualDeliveryDate = actualDeliveryDate; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public String getProductsSummary() { return productsSummary; }
    public void setProductsSummary(String productsSummary) { this.productsSummary = productsSummary; }

    public List<ShipmentTrackingEventDTO> getTrackingTimeline() { return trackingTimeline; }
    public void setTrackingTimeline(List<ShipmentTrackingEventDTO> trackingTimeline) { this.trackingTimeline = trackingTimeline; }

    public List<ShipmentTrackingEventDTO> getTrackingEvents() { return trackingTimeline; }
    public void setTrackingEvents(List<ShipmentTrackingEventDTO> trackingEvents) { this.trackingTimeline = trackingEvents; }

    public String getDeliveryPartnerName() { return courier; }
    public void setDeliveryPartnerName(String deliveryPartnerName) { this.courier = deliveryPartnerName; }

    public List<FulfillmentPackageDTO> getPackages() { return packages; }
    public void setPackages(List<FulfillmentPackageDTO> packages) { this.packages = packages; }
}
