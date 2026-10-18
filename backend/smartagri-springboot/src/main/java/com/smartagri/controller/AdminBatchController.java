package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.service.BatchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory/batches")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminBatchController {

    private final BatchService batchService;

    public AdminBatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    private String getAuthenticatedUsername(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return (username != null && !username.isBlank()) ? username : "admin";
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<BatchSummaryDTO>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(batchService.getSummary()));
    }

    @GetMapping("/near-expiry")
    public ResponseEntity<ApiResponse<List<BatchDTO>>> getNearExpiryBatches() {
        return ResponseEntity.ok(ApiResponse.ok(batchService.getNearExpiryBatches()));
    }

    @GetMapping("/expired")
    public ResponseEntity<ApiResponse<List<BatchDTO>>> getExpiredBatches() {
        return ResponseEntity.ok(ApiResponse.ok(batchService.getExpiredBatches()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BatchDTO>>> getAllBatches(
            @RequestParam(required = false) Long fertilizerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String expiryFilter) {
        return ResponseEntity.ok(ApiResponse.ok(batchService.getAllBatches(fertilizerId, status, search, expiryFilter)));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<BatchDTO>> getBatchById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(batchService.getBatch(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BatchDTO>> createBatch(
            HttpServletRequest request,
            @Valid @RequestBody CreateBatchRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        BatchDTO created = batchService.createBatch(body, adminUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Batch #" + created.getBatchNumber() + " created successfully", created));
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<BatchDTO>> updateBatch(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody UpdateBatchRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        BatchDTO updated = batchService.updateBatch(id, body, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Batch #" + updated.getBatchNumber() + " updated successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/block")
    public ResponseEntity<ApiResponse<BatchDTO>> blockBatch(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody BatchBlockRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        BatchDTO blocked = batchService.blockBatch(id, body.getReason(), adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Batch #" + blocked.getBatchNumber() + " has been blocked from sale", blocked));
    }

    @PostMapping("/{id:[0-9]+}/unblock")
    public ResponseEntity<ApiResponse<BatchDTO>> unblockBatch(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        BatchDTO unblocked = batchService.unblockBatch(id, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Batch #" + unblocked.getBatchNumber() + " unblocked successfully", unblocked));
    }

    @PostMapping("/{id:[0-9]+}/adjust")
    public ResponseEntity<ApiResponse<BatchDTO>> adjustBatch(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody BatchAdjustmentRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        BatchDTO adjusted = batchService.adjustBatch(id, body, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Batch #" + adjusted.getBatchNumber() + " adjusted successfully", adjusted));
    }

    @PostMapping("/{id:[0-9]+}/damaged")
    public ResponseEntity<ApiResponse<BatchDTO>> markDamaged(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody BatchDamageRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        BatchDTO damaged = batchService.markDamaged(id, body, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Batch #" + damaged.getBatchNumber() + " damaged stock written off successfully", damaged));
    }

    @GetMapping("/{id:[0-9]+}/history")
    public ResponseEntity<ApiResponse<List<InventoryTransactionDTO>>> getBatchHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(batchService.getBatchHistory(id)));
    }
}
