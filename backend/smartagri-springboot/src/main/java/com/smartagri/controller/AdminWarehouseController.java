package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.entity.WarehouseStatus;
import com.smartagri.service.WarehouseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/warehouses")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminWarehouseController {

    private final WarehouseService warehouseService;

    public AdminWarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    private String getAuthenticatedUsername(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return (username != null && !username.isBlank()) ? username : "admin";
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getAllWarehouses(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getAllWarehouses(status, search, type)));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<WarehouseDashboardDTO>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getDashboard()));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<WarehouseDTO>> getWarehouseById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getWarehouse(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseDTO>> createWarehouse(
            HttpServletRequest request,
            @Valid @RequestBody CreateWarehouseRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        WarehouseDTO created = warehouseService.createWarehouse(body, adminUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Warehouse '" + created.getWarehouseCode() + "' created successfully", created));
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<WarehouseDTO>> updateWarehouse(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        WarehouseDTO updated = warehouseService.updateWarehouse(id, body, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Warehouse '" + updated.getWarehouseCode() + "' updated successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/activate")
    public ResponseEntity<ApiResponse<WarehouseDTO>> activateWarehouse(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        return ResponseEntity.ok(ApiResponse.ok("Warehouse activated successfully", warehouseService.activateWarehouse(id, adminUser)));
    }

    @PostMapping("/{id:[0-9]+}/deactivate")
    public ResponseEntity<ApiResponse<WarehouseDTO>> deactivateWarehouse(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        return ResponseEntity.ok(ApiResponse.ok("Warehouse deactivated successfully", warehouseService.deactivateWarehouse(id, adminUser)));
    }

    @RequestMapping(value = "/{id:[0-9]+}/status", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<WarehouseDTO>> updateWarehouseStatus(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam String status) {
        String adminUser = getAuthenticatedUsername(request);
        WarehouseStatus target = WarehouseStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT));
        WarehouseDTO res;
        if (target == WarehouseStatus.ACTIVE) {
            res = warehouseService.activateWarehouse(id, adminUser);
        } else if (target == WarehouseStatus.INACTIVE) {
            res = warehouseService.deactivateWarehouse(id, adminUser);
        } else if (target == WarehouseStatus.BLOCKED) {
            res = warehouseService.blockWarehouse(id, adminUser);
        } else {
            throw new IllegalArgumentException("Unsupported status: " + status);
        }
        return ResponseEntity.ok(ApiResponse.ok("Warehouse status updated to " + target, res));
    }

    @PostMapping("/{id:[0-9]+}/default")
    public ResponseEntity<ApiResponse<WarehouseDTO>> setDefaultReceivingWarehouse(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        return ResponseEntity.ok(ApiResponse.ok("Default receiving warehouse set", warehouseService.setDefaultReceivingWarehouse(id, adminUser)));
    }

    @GetMapping({ "/{id:[0-9]+}/inventory", "/{id:[0-9]+}/stock" })
    public ResponseEntity<ApiResponse<List<BatchLocationStockDTO>>> getWarehouseInventory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getWarehouseInventory(id)));
    }

    @GetMapping("/{id:[0-9]+}/transfers")
    public ResponseEntity<ApiResponse<List<StockTransferDTO>>> getWarehouseTransfers(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getWarehouseTransfers(id)));
    }

    @GetMapping("/{warehouseId:[0-9]+}/locations")
    public ResponseEntity<ApiResponse<List<WarehouseLocationDTO>>> getWarehouseLocations(
            @PathVariable Long warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getLocationsByWarehouse(warehouseId, status, type)));
    }

    @PostMapping("/{warehouseId:[0-9]+}/locations")
    public ResponseEntity<ApiResponse<WarehouseLocationDTO>> createWarehouseLocation(
            HttpServletRequest request,
            @PathVariable Long warehouseId,
            @Valid @RequestBody CreateWarehouseLocationRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        WarehouseLocationDTO created = warehouseService.createLocation(warehouseId, body, adminUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Location '" + created.getLocationCode() + "' created successfully", created));
    }
}
