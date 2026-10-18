package com.smartagri.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdatePaymentStatusRequest {

    @NotBlank(message = "Payment status is required")
    private String paymentStatus;

    private String transactionId;
    private String gatewayResponse;
    private String failureReason;

    public UpdatePaymentStatusRequest() {}

    public UpdatePaymentStatusRequest(String paymentStatus, String transactionId, String gatewayResponse) {
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
        this.gatewayResponse = gatewayResponse;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
