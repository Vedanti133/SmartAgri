package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.service.StockTransferService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/stock-transfers", "/api/admin/transfers"})
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminStockTransferController {

    private final StockTransferService stockTransferService;

    public AdminStockTransferController(StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }

    private String getAuthenticatedUsername(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return (username != null && !username.isBlank()) ? username : "admin";
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StockTransferDTO>>> getAllTransfers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long sourceWarehouseId,
            @RequestParam(required = false) Long destinationWarehouseId) {
        return ResponseEntity.ok(ApiResponse.ok(stockTransferService.getAllTransfers(status, sourceWarehouseId, destinationWarehouseId)));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<StockTransferDTO>> getTransferById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(stockTransferService.getTransfer(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockTransferDTO>> createTransfer(
            HttpServletRequest request,
            @Valid @RequestBody CreateStockTransferRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        StockTransferDTO created = stockTransferService.createTransfer(body, adminUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Stock Transfer #" + created.getTransferNumber() + " created successfully", created));
    }

    @PostMapping("/{id:[0-9]+}/submit")
    public ResponseEntity<ApiResponse<StockTransferDTO>> submitTransfer(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        StockTransferDTO submitted = stockTransferService.submitTransfer(id, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Transfer submitted for approval", submitted));
    }

    @PostMapping("/{id:[0-9]+}/approve")
    public ResponseEntity<ApiResponse<StockTransferDTO>> approveTransfer(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        StockTransferDTO approved = stockTransferService.approveTransfer(id, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Transfer approved successfully", approved));
    }

    @PostMapping("/{id:[0-9]+}/dispatch")
    public ResponseEntity<ApiResponse<StockTransferDTO>> dispatchTransfer(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        StockTransferDTO dispatched = stockTransferService.dispatchTransfer(id, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Transfer dispatched successfully", dispatched));
    }

    @PostMapping("/{id:[0-9]+}/receive")
    public ResponseEntity<ApiResponse<StockTransferDTO>> receiveTransfer(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) ReceiveStockTransferRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        StockTransferDTO received = stockTransferService.receiveTransfer(id, body, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Transfer received successfully", received));
    }

    @PostMapping("/{id:[0-9]+}/cancel")
    public ResponseEntity<ApiResponse<StockTransferDTO>> cancelTransfer(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) Map<String, String> body) {
        String adminUser = getAuthenticatedUsername(request);
        String finalReason = (reason != null && !reason.isBlank()) ? reason : (body != null ? body.get("reason") : null);
        StockTransferDTO cancelled = stockTransferService.cancelTransfer(id, finalReason, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Transfer cancelled successfully", cancelled));
    }

    @PostMapping("/{id:[0-9]+}/reject")
    public ResponseEntity<ApiResponse<StockTransferDTO>> rejectTransfer(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) Map<String, String> body) {
        String adminUser = getAuthenticatedUsername(request);
        String finalReason = (reason != null && !reason.isBlank()) ? reason : (body != null ? body.get("reason") : null);
        StockTransferDTO rejected = stockTransferService.rejectTransfer(id, finalReason, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Transfer rejected successfully", rejected));
    }
}
