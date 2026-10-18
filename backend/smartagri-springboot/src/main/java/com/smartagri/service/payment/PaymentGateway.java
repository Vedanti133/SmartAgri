package com.smartagri.service.payment;

import java.math.BigDecimal;

public interface PaymentGateway {
    String getGatewayName();

    GatewayOrder createOrder(Long orderId, String orderNumber, BigDecimal amount, String customerEmail, String customerPhone);

    GatewayResult processPayment(Long orderId, String orderNumber, BigDecimal amount, String paymentReference);

    GatewayResult verifyPayment(String transactionId, String paymentReference);

    GatewayResult verifyPaymentSignature(String gatewayOrderId, String gatewayPaymentId, String gatewaySignature, BigDecimal expectedAmount);

    GatewayResult refundPayment(String transactionId, BigDecimal amount);

    record GatewayOrder(String gatewayOrderId, BigDecimal amount, String currency, String keyId, String status) {}

    record GatewayResult(boolean success, String transactionId, String gatewayPaymentId, String gatewayResponse, String message) {
        public GatewayResult(boolean success, String transactionId, String gatewayResponse, String message) {
            this(success, transactionId, null, gatewayResponse, message);
        }
    }
}
