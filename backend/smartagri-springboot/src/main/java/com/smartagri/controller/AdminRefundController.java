package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.ProcessRefundRequest;
import com.smartagri.dto.RefundDTO;
import com.smartagri.enums.RefundStatus;
import com.smartagri.service.RefundService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/refunds")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminRefundController {

    private final RefundService refundService;

    public AdminRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RefundDTO>>> getAllRefunds(
            @RequestParam(required = false) RefundStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.getAllRefunds(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RefundDTO>> getRefundById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.getRefundById(id)));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<RefundDTO>> processRefund(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) ProcessRefundRequest body) {
        String adminUser = (String) request.getAttribute("currentUsername");
        RefundDTO res = refundService.processRefund(id, body, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Refund #" + res.getRefundNumber() + " processed successfully", res));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<RefundDTO>> retryRefund(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = (String) request.getAttribute("currentUsername");
        RefundDTO res = refundService.retryRefund(id, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Refund retry initiated for #" + res.getRefundNumber(), res));
    }
}
