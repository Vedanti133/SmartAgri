package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.ConfirmPaymentRequest;
import com.smartagri.dto.PaymentDTO;
import com.smartagri.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ------------------------------------------------------------------------
    // Legacy / Phase 5 Order-Scoped Endpoints (Backward Compatible)
    // ------------------------------------------------------------------------

    @GetMapping("/api/orders/{id}/payment")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPayment(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("currentUserId");
        PaymentDTO payment = paymentService.getPaymentForOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Payment details retrieved successfully", payment));
    }

    @PostMapping("/api/orders/{id}/payment/pay-online")
    public ResponseEntity<ApiResponse<PaymentDTO>> payOnline(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload) {
        Long userId = (Long) request.getAttribute("currentUserId");
        String paymentReference = payload != null ? payload.get("paymentReference") : null;
        PaymentDTO payment = paymentService.processOnlinePaymentMock(userId, id, paymentReference);
        return ResponseEntity.ok(ApiResponse.ok("Online payment processed successfully (Test Mode)", payment));
    }

    // ------------------------------------------------------------------------
    // Phase 9 Dedicated Payment Endpoints
    // ------------------------------------------------------------------------

    @GetMapping("/api/payments/my")
    public ResponseEntity<ApiResponse<List<PaymentDTO>>> getMyPayments(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        List<PaymentDTO> payments = paymentService.getCustomerPayments(userId);
        return ResponseEntity.ok(ApiResponse.ok("Customer payments retrieved successfully", payments));
    }

    @GetMapping("/api/payments/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentByOrder(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        PaymentDTO payment = paymentService.getPaymentForOrder(userId, orderId);
        return ResponseEntity.ok(ApiResponse.ok("Payment details retrieved successfully", payment));
    }

    @GetMapping("/api/payments/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentById(HttpServletRequest request, @PathVariable Long paymentId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        PaymentDTO payment = paymentService.getPaymentById(userId, paymentId);
        return ResponseEntity.ok(ApiResponse.ok("Payment retrieved successfully", payment));
    }

    @PostMapping("/api/payments/order/{orderId}/initiate")
    public ResponseEntity<ApiResponse<PaymentDTO>> initiatePayment(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        PaymentDTO payment = paymentService.initiateOnlinePayment(userId, orderId);
        return ResponseEntity.ok(ApiResponse.ok("Online payment initiated successfully", payment));
    }

    @PostMapping("/api/payments/{paymentId}/confirm")
    public ResponseEntity<ApiResponse<PaymentDTO>> confirmPayment(
            HttpServletRequest request,
            @PathVariable Long paymentId,
            @RequestBody(required = false) ConfirmPaymentRequest body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        PaymentDTO payment = paymentService.confirmPayment(userId, paymentId, body);
        return ResponseEntity.ok(ApiResponse.ok("Payment confirmed successfully", payment));
    }

    @PostMapping("/api/payments/{paymentId}/retry")
    public ResponseEntity<ApiResponse<PaymentDTO>> retryPayment(
            HttpServletRequest request,
            @PathVariable Long paymentId,
            @RequestBody(required = false) Map<String, String> payload) {
        Long userId = (Long) request.getAttribute("currentUserId");
        PaymentDTO payment = paymentService.getPaymentById(userId, paymentId);
        String paymentReference = payload != null ? payload.get("paymentReference") : null;
        PaymentDTO retried = paymentService.retryPayment(userId, payment.getOrderId(), paymentReference);
        return ResponseEntity.ok(ApiResponse.ok("Payment retry initiated successfully", retried));
    }

    @PostMapping("/api/payments/order/{orderId}/retry")
    public ResponseEntity<ApiResponse<PaymentDTO>> retryPaymentByOrder(
            HttpServletRequest request,
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> payload) {
        Long userId = (Long) request.getAttribute("currentUserId");
        String paymentReference = payload != null ? payload.get("paymentReference") : null;
        PaymentDTO retried = paymentService.retryPayment(userId, orderId, paymentReference);
        return ResponseEntity.ok(ApiResponse.ok("Payment retry initiated successfully", retried));
    }
}
