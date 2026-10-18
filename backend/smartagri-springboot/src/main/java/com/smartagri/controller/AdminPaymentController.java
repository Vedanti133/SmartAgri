package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.PaymentDTO;
import com.smartagri.dto.UpdatePaymentStatusRequest;
import com.smartagri.entity.PaymentStatus;
import com.smartagri.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/payments")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentDTO>>> getAllPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String search) {
        PaymentStatus paymentStatus = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            try {
                paymentStatus = PaymentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }
        String cleanMethod = (method != null && !method.isBlank() && !"ALL".equalsIgnoreCase(method.trim()))
                ? method.trim() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        List<PaymentDTO> payments = paymentService.getAllPaymentsAdmin(paymentStatus, cleanMethod, cleanSearch);
        return ResponseEntity.ok(ApiResponse.ok("Admin payments retrieved successfully", payments));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getOne(@PathVariable Long orderId) {
        PaymentDTO payment = paymentService.getPaymentAdmin(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Payment retrieved successfully", payment));
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getByPaymentId(@PathVariable Long paymentId) {
        PaymentDTO payment = paymentService.getPaymentAdminById(paymentId);
        return ResponseEntity.ok(ApiResponse.ok("Payment retrieved successfully", payment));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<PaymentDTO>> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        PaymentDTO updated = paymentService.updatePaymentStatusAdmin(orderId, request);
        return ResponseEntity.ok(ApiResponse.ok("Payment status updated successfully", updated));
    }
}
