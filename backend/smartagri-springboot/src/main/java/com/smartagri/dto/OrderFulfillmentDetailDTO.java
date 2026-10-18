package com.smartagri.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderFulfillmentDetailDTO {
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private String city;
    private String state;
    private String pincode;
    private String orderStatus;
    private String fulfillmentStatus;
    private String paymentStatus;
    private String paymentMethod;
    private BigDecimal grandTotal;
    private LocalDateTime orderDate;

    private List<FulfillmentAllocationDTO> allocations = new ArrayList<>();
    private List<PickListDTO> pickLists = new ArrayList<>();
    private List<FulfillmentPackageDTO> packages = new ArrayList<>();
    private List<ShipmentDTO> shipments = new ArrayList<>();

    public OrderFulfillmentDetailDTO() {}

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getFulfillmentStatus() { return fulfillmentStatus; }
    public void setFulfillmentStatus(String fulfillmentStatus) { this.fulfillmentStatus = fulfillmentStatus; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public List<FulfillmentAllocationDTO> getAllocations() { return allocations; }
    public void setAllocations(List<FulfillmentAllocationDTO> allocations) { this.allocations = allocations; }

    public List<PickListDTO> getPickLists() { return pickLists; }
    public void setPickLists(List<PickListDTO> pickLists) { this.pickLists = pickLists; }

    public List<FulfillmentPackageDTO> getPackages() { return packages; }
    public void setPackages(List<FulfillmentPackageDTO> packages) { this.packages = packages; }

    public List<ShipmentDTO> getShipments() { return shipments; }
    public void setShipments(List<ShipmentDTO> shipments) { this.shipments = shipments; }
}
