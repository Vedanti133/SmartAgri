package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.service.PackageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/packages")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminPackageController {

    private final PackageService packageService;

    public AdminPackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FulfillmentPackageDTO>>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long warehouseId) {
        List<FulfillmentPackageDTO> list = packageService.getPackages(status, orderId, warehouseId);
        return ResponseEntity.ok(ApiResponse.ok("Packages retrieved successfully", list));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<FulfillmentPackageDTO>> getOne(@PathVariable Long id) {
        FulfillmentPackageDTO dto = packageService.getPackageById(id);
        return ResponseEntity.ok(ApiResponse.ok("Package retrieved successfully", dto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FulfillmentPackageDTO>> create(
            HttpServletRequest request,
            @Valid @RequestBody CreatePackageRequest createReq) {
        String username = (String) request.getAttribute("currentUsername");
        FulfillmentPackageDTO created = packageService.createPackage(createReq, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Package created successfully", created));
    }

    @PostMapping({ "/{id:[0-9]+}/pack", "/{id:[0-9]+}/finalize" })
    public ResponseEntity<ApiResponse<FulfillmentPackageDTO>> markPacked(
            HttpServletRequest request,
            @PathVariable Long id) {
        String username = (String) request.getAttribute("currentUsername");
        FulfillmentPackageDTO updated = packageService.markPackagePacked(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Package marked as PACKED successfully", updated));
    }
}
