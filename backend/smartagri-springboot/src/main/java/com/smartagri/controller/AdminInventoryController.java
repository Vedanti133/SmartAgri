package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final com.smartagri.service.WarehouseService warehouseService;

    public AdminInventoryController(InventoryService inventoryService, com.smartagri.service.WarehouseService warehouseService) {
        this.inventoryService = inventoryService;
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getInventory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size);
            Page<InventoryDTO> pagedResult = inventoryService.getInventoryPaged(status, category, search, pageable);
            return ResponseEntity.ok(ApiResponse.ok("Inventory retrieved successfully", pagedResult));
        }

        List<InventoryDTO> list = inventoryService.getAllInventory(status, category, search);
        return ResponseEntity.ok(ApiResponse.ok("Inventory retrieved successfully", list));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<InventorySummaryDTO>> getSummary() {
        InventorySummaryDTO summary = inventoryService.getSummary();
        return ResponseEntity.ok(ApiResponse.ok("Inventory summary retrieved successfully", summary));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<InventoryHealthDTO>> getHealth() {
        InventoryHealthDTO health = inventoryService.getHealth();
        return ResponseEntity.ok(ApiResponse.ok("Inventory consistency check completed", health));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryDTO>>> getLowStockItems() {
        List<InventoryDTO> items = inventoryService.getLowStockItems();
        return ResponseEntity.ok(ApiResponse.ok("Low stock items retrieved successfully", items));
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<ApiResponse<List<InventoryDTO>>> getOutOfStockItems() {
        List<InventoryDTO> items = inventoryService.getOutOfStockItems();
        return ResponseEntity.ok(ApiResponse.ok("Out of stock items retrieved successfully", items));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<InventoryTransactionDTO>>> getAllTransactions(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {
        List<InventoryTransactionDTO> txs = inventoryService.getTransactions(productId, type, search);
        return ResponseEntity.ok(ApiResponse.ok("Inventory transactions retrieved successfully", txs));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryDTO>> getProductInventory(@PathVariable Long productId) {
        InventoryDTO dto = inventoryService.getProductInventory(productId);
        return ResponseEntity.ok(ApiResponse.ok("Product inventory retrieved successfully", dto));
    }

    @GetMapping("/{productId}/transactions")
    public ResponseEntity<ApiResponse<List<InventoryTransactionDTO>>> getProductTransactions(@PathVariable Long productId) {
        List<InventoryTransactionDTO> txs = inventoryService.getProductTransactions(productId);
        return ResponseEntity.ok(ApiResponse.ok("Product transaction history retrieved successfully", txs));
    }

    @PostMapping("/stock-in")
    public ResponseEntity<ApiResponse<InventoryDTO>> stockInDirect(
            HttpServletRequest request,
            @Valid @RequestBody StockInRequest body) {
        Long pId = body.getFertilizerId();
        if (pId == null) {
            throw new IllegalArgumentException("Product ID / Fertilizer ID is required");
        }
        String username = (String) request.getAttribute("currentUsername");
        if (username == null || username.isBlank()) {
            username = "admin";
        }
        InventoryDTO updated = inventoryService.addStock(pId, body, username);
        return ResponseEntity.ok(ApiResponse.ok("Stock added successfully", updated));
    }

    @PostMapping("/{productId}/stock-in")
    public ResponseEntity<ApiResponse<InventoryDTO>> stockIn(
            HttpServletRequest request,
            @PathVariable Long productId,
            @Valid @RequestBody StockInRequest body) {
        String username = (String) request.getAttribute("currentUsername");
        if (username == null || username.isBlank()) {
            username = "admin";
        }
        InventoryDTO updated = inventoryService.addStock(productId, body, username);
        return ResponseEntity.ok(ApiResponse.ok("Stock added successfully", updated));
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<InventoryDTO>> adjustStockDirect(
            HttpServletRequest request,
            @Valid @RequestBody StockAdjustmentRequest body) {
        Long pId = body.getFertilizerId();
        if (pId == null) {
            throw new IllegalArgumentException("Product ID / Fertilizer ID is required");
        }
        String username = (String) request.getAttribute("currentUsername");
        if (username == null || username.isBlank()) {
            username = "admin";
        }
        InventoryDTO updated = inventoryService.adjustStock(pId, body, username);
        return ResponseEntity.ok(ApiResponse.ok("Stock adjusted successfully", updated));
    }

    @PostMapping("/{productId}/adjust")
    public ResponseEntity<ApiResponse<InventoryDTO>> adjustStock(
            HttpServletRequest request,
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest body) {
        String username = (String) request.getAttribute("currentUsername");
        if (username == null || username.isBlank()) {
            username = "admin";
        }
        InventoryDTO updated = inventoryService.adjustStock(productId, body, username);
        return ResponseEntity.ok(ApiResponse.ok("Stock adjusted successfully", updated));
    }

    @GetMapping("/warehouses")
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getWarehouseInventorySummary() {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getAllWarehouses(null, null, null)));
    }

    @GetMapping("/warehouses/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<WarehouseDTO>> getWarehouseInventoryDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getWarehouse(id)));
    }

    @GetMapping("/warehouses/{id:[0-9]+}/stock")
    public ResponseEntity<ApiResponse<List<BatchLocationStockDTO>>> getWarehouseStock(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getWarehouseInventory(id)));
    }

    @GetMapping("/warehouses/{id:[0-9]+}/low-stock")
    public ResponseEntity<ApiResponse<List<BatchLocationStockDTO>>> getWarehouseLowStock(@PathVariable Long id) {
        List<BatchLocationStockDTO> stock = warehouseService.getWarehouseInventory(id).stream()
                .filter(s -> s.getQuantity() != null && s.getQuantity() <= 10)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(stock));
    }

    @GetMapping("/warehouses/{id:[0-9]+}/expiring")
    public ResponseEntity<ApiResponse<List<BatchLocationStockDTO>>> getWarehouseExpiringStock(@PathVariable Long id) {
        java.time.LocalDate threshold = java.time.LocalDate.now().plusDays(30);
        List<BatchLocationStockDTO> stock = warehouseService.getWarehouseInventory(id).stream()
                .filter(s -> s.getExpiryDate() != null && !s.getExpiryDate().isAfter(threshold))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(stock));
    }

    @GetMapping("/locations/{id:[0-9]+}/stock")
    public ResponseEntity<ApiResponse<List<BatchLocationStockDTO>>> getLocationStock(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getLocationInventory(id)));
    }
}
