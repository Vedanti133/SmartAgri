package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.service.WarehouseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/locations")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminLocationController {

    private final WarehouseService warehouseService;

    public AdminLocationController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    private String getAuthenticatedUsername(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return (username != null && !username.isBlank()) ? username : "admin";
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<WarehouseLocationDTO>> getLocationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getLocation(id)));
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<WarehouseLocationDTO>> updateLocation(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseLocationRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        WarehouseLocationDTO updated = warehouseService.updateLocation(id, body, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Location '" + updated.getLocationCode() + "' updated successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/activate")
    public ResponseEntity<ApiResponse<WarehouseLocationDTO>> activateLocation(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        return ResponseEntity.ok(ApiResponse.ok("Location activated successfully", warehouseService.activateLocation(id, adminUser)));
    }

    @PostMapping("/{id:[0-9]+}/deactivate")
    public ResponseEntity<ApiResponse<WarehouseLocationDTO>> deactivateLocation(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        return ResponseEntity.ok(ApiResponse.ok("Location deactivated successfully", warehouseService.deactivateLocation(id, adminUser)));
    }

    @PostMapping("/{id:[0-9]+}/block")
    public ResponseEntity<ApiResponse<WarehouseLocationDTO>> blockLocation(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        return ResponseEntity.ok(ApiResponse.ok("Location blocked successfully", warehouseService.blockLocation(id, adminUser)));
    }

    @RequestMapping(value = "/{id:[0-9]+}/status", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<WarehouseLocationDTO>> updateLocationStatus(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam String status) {
        String adminUser = getAuthenticatedUsername(request);
        com.smartagri.entity.LocationStatus target = com.smartagri.entity.LocationStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT));
        WarehouseLocationDTO res;
        if (target == com.smartagri.entity.LocationStatus.ACTIVE) {
            res = warehouseService.activateLocation(id, adminUser);
        } else if (target == com.smartagri.entity.LocationStatus.BLOCKED) {
            res = warehouseService.blockLocation(id, adminUser);
        } else if (target == com.smartagri.entity.LocationStatus.INACTIVE) {
            res = warehouseService.deactivateLocation(id, adminUser);
        } else {
            throw new IllegalArgumentException("Unsupported status: " + status);
        }
        return ResponseEntity.ok(ApiResponse.ok("Location status updated to " + target, res));
    }

    @GetMapping({ "/{id:[0-9]+}/inventory", "/{id:[0-9]+}/stock" })
    public ResponseEntity<ApiResponse<List<BatchLocationStockDTO>>> getLocationInventory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getLocationInventory(id)));
    }
}
