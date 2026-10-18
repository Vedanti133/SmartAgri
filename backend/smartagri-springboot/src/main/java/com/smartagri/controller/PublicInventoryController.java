package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.StockStatusDTO;
import com.smartagri.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class PublicInventoryController {

    private final InventoryService inventoryService;

    public PublicInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/status/{productId}")
    public ResponseEntity<ApiResponse<StockStatusDTO>> getProductStockStatus(@PathVariable Long productId) {
        StockStatusDTO status = inventoryService.getCustomerStockStatus(productId);
        return ResponseEntity.ok(ApiResponse.ok("Stock status retrieved successfully", status));
    }
}
