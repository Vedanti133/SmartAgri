package com.smartagri.service.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public String getGatewayName() {
        return "MOCK_GATEWAY (TEST MODE)";
    }

    @Override
    public GatewayOrder createOrder(Long orderId, String orderNumber, BigDecimal amount, String customerEmail, String customerPhone) {
        String datePart = LocalDate.now().format(DATE_FMT);
        int randomSeq = ThreadLocalRandom.current().nextInt(100000, 999999);
        String gatewayOrderId = String.format(Locale.ROOT, "order_mock_%s_%06d", datePart, randomSeq);
        return new GatewayOrder(gatewayOrderId, amount, "INR", "mock_key_id_smartagri", "created");
    }

    @Override
    public GatewayResult processPayment(Long orderId, String orderNumber, BigDecimal amount, String paymentReference) {
        String datePart = LocalDate.now().format(DATE_FMT);
        int randomSeq = ThreadLocalRandom.current().nextInt(100000, 999999);
        String txnId = String.format(Locale.ROOT, "TXN-TEST-%s-%06d", datePart, randomSeq);
        String payId = String.format(Locale.ROOT, "pay_mock_%s_%06d", datePart, randomSeq);

        if (paymentReference != null && (paymentReference.contains("FAIL") || paymentReference.contains("DECLINE"))) {
            String response = String.format(Locale.ROOT, "{\"status\":\"FAILED\",\"mode\":\"TEST\",\"ref\":\"%s\",\"reason\":\"Declined by issuing bank (Test Simulation)\"}",
                    paymentReference);
            return new GatewayResult(false, txnId, payId, response, "Payment declined by issuing bank (Test Simulation)");
        }

        String response = String.format(Locale.ROOT, "{\"status\":\"SUCCESS\",\"mode\":\"TEST\",\"ref\":\"%s\",\"amount\":\"%s\",\"payId\":\"%s\"}",
                paymentReference != null ? paymentReference : "NONE", amount.toPlainString(), payId);
        return new GatewayResult(true, txnId, payId, response, "Payment processed successfully in test mode");
    }

    @Override
    public GatewayResult verifyPayment(String transactionId, String paymentReference) {
        return new GatewayResult(true, transactionId, "{\"verified\":true}", "Payment verified in test mode");
    }

    @Override
    public GatewayResult verifyPaymentSignature(String gatewayOrderId, String gatewayPaymentId, String gatewaySignature, BigDecimal expectedAmount) {
        if (gatewaySignature != null && gatewaySignature.contains("INVALID")) {
            return new GatewayResult(false, null, gatewayPaymentId, "{\"error\":\"Signature verification failed\"}", "Invalid payment signature");
        }
        String datePart = LocalDate.now().format(DATE_FMT);
        int randomSeq = ThreadLocalRandom.current().nextInt(100000, 999999);
        String txnId = String.format(Locale.ROOT, "TXN-CONF-%s-%06d", datePart, randomSeq);
        String response = String.format(Locale.ROOT, "{\"status\":\"VERIFIED\",\"gatewayOrderId\":\"%s\",\"gatewayPaymentId\":\"%s\"}",
                gatewayOrderId, gatewayPaymentId);
        return new GatewayResult(true, txnId, gatewayPaymentId, response, "Payment verified successfully in test mode");
    }

    @Override
    public GatewayResult refundPayment(String transactionId, BigDecimal amount) {
        String refundId = "RFND-" + transactionId;
        return new GatewayResult(true, refundId, "{\"refundStatus\":\"SUCCESS\"}", "Refund processed in test mode");
    }
}
