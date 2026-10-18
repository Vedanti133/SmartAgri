package com.smartagri.dto;

import jakarta.validation.constraints.NotNull;

public class CreatePaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private String paymentMethod = "COD";
    private String paymentReference;

    public CreatePaymentRequest() {}

    public CreatePaymentRequest(Long orderId, String paymentMethod, String paymentReference) {
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }
}
