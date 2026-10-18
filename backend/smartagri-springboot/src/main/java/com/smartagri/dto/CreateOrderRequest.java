package com.smartagri.dto;

import jakarta.validation.constraints.NotNull;

public class CreateOrderRequest {

    @NotNull(message = "Delivery address is required")
    private Long addressId;

    private String paymentMethod = "COD";
    private String notes;
    private String paymentReference;

    private java.math.BigDecimal expectedGrandTotal;
    private java.math.BigDecimal expectedSubtotal;
    private java.util.Map<Long, java.math.BigDecimal> expectedItemPrices;

    public CreateOrderRequest() {}

    public CreateOrderRequest(Long addressId, String paymentMethod, String notes) {
        this.addressId = addressId;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }

    public CreateOrderRequest(Long addressId, String paymentMethod, String notes, String paymentReference) {
        this.addressId = addressId;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
        this.paymentReference = paymentReference;
    }

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public java.math.BigDecimal getExpectedGrandTotal() { return expectedGrandTotal; }
    public void setExpectedGrandTotal(java.math.BigDecimal expectedGrandTotal) { this.expectedGrandTotal = expectedGrandTotal; }

    public java.math.BigDecimal getExpectedSubtotal() { return expectedSubtotal; }
    public void setExpectedSubtotal(java.math.BigDecimal expectedSubtotal) { this.expectedSubtotal = expectedSubtotal; }

    public java.util.Map<Long, java.math.BigDecimal> getExpectedItemPrices() { return expectedItemPrices; }
    public void setExpectedItemPrices(java.util.Map<Long, java.math.BigDecimal> expectedItemPrices) { this.expectedItemPrices = expectedItemPrices; }
}
