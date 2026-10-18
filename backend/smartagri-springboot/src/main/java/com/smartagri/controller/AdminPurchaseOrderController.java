package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.entity.PurchaseOrderStatus;
import com.smartagri.service.PurchaseOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/purchase-orders")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminPurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public AdminPurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    private String getAuthenticatedUsername(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return (username != null && !username.isBlank()) ? username : "admin";
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrderDTO>>> getAllPurchaseOrders(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(purchaseOrderService.getAllPurchaseOrders(supplierId, status, search)));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> getPurchaseOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(purchaseOrderService.getPurchaseOrderById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> createPurchaseOrder(
            HttpServletRequest request,
            @Valid @RequestBody CreatePurchaseOrderRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        PurchaseOrderDTO created = purchaseOrderService.createPurchaseOrder(body, adminUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Purchase Order #" + created.getPoNumber() + " created successfully", created));
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> updatePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePurchaseOrderRequest body) {
        PurchaseOrderDTO updated = purchaseOrderService.updatePurchaseOrder(id, body);
        return ResponseEntity.ok(ApiResponse.ok("Purchase Order #" + updated.getPoNumber() + " updated successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/submit")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> submitForApproval(@PathVariable Long id) {
        PurchaseOrderDTO submitted = purchaseOrderService.submitForApproval(id);
        return ResponseEntity.ok(ApiResponse.ok("Purchase Order #" + submitted.getPoNumber() + " submitted for approval", submitted));
    }

    @PostMapping("/{id:[0-9]+}/approve")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> approvePurchaseOrder(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        PurchaseOrderDTO approved = purchaseOrderService.approvePurchaseOrder(id, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Purchase Order #" + approved.getPoNumber() + " approved successfully", approved));
    }

    @PostMapping("/{id:[0-9]+}/send")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> sendToSupplier(@PathVariable Long id) {
        PurchaseOrderDTO sent = purchaseOrderService.sendToSupplier(id);
        return ResponseEntity.ok(ApiResponse.ok("Purchase Order #" + sent.getPoNumber() + " sent to supplier", sent));
    }

    @PostMapping("/{id:[0-9]+}/cancel")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> cancelPurchaseOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null) ? body.get("reason") : null;
        PurchaseOrderDTO cancelled = purchaseOrderService.cancelPurchaseOrder(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Purchase Order #" + cancelled.getPoNumber() + " cancelled", cancelled));
    }

    @PostMapping("/{id:[0-9]+}/close")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> closePurchaseOrder(@PathVariable Long id) {
        PurchaseOrderDTO closed = purchaseOrderService.closePurchaseOrder(id);
        return ResponseEntity.ok(ApiResponse.ok("Purchase Order #" + closed.getPoNumber() + " closed", closed));
    }
}
