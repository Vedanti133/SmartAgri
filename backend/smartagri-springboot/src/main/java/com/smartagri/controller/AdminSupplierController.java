package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.CreateSupplierRequest;
import com.smartagri.dto.PurchaseOrderDTO;
import com.smartagri.dto.SupplierDTO;
import com.smartagri.dto.UpdateSupplierRequest;
import com.smartagri.entity.SupplierStatus;
import com.smartagri.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/suppliers")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminSupplierController {

    private final SupplierService supplierService;

    public AdminSupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupplierDTO>>> getAllSuppliers(
            @RequestParam(required = false) SupplierStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.getAllSuppliers(status, search)));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<SupplierDTO>> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.getSupplierById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierDTO>> createSupplier(@Valid @RequestBody CreateSupplierRequest body) {
        SupplierDTO created = supplierService.createSupplier(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Supplier '" + created.getCompanyName() + "' created successfully", created));
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<SupplierDTO>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplierRequest body) {
        SupplierDTO updated = supplierService.updateSupplier(id, body);
        return ResponseEntity.ok(ApiResponse.ok("Supplier '" + updated.getCompanyName() + "' updated successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/activate")
    public ResponseEntity<ApiResponse<SupplierDTO>> activateSupplier(@PathVariable Long id) {
        SupplierDTO activated = supplierService.activateSupplier(id);
        return ResponseEntity.ok(ApiResponse.ok("Supplier activated successfully", activated));
    }

    @PostMapping("/{id:[0-9]+}/deactivate")
    public ResponseEntity<ApiResponse<SupplierDTO>> deactivateSupplier(@PathVariable Long id) {
        SupplierDTO deactivated = supplierService.deactivateSupplier(id);
        return ResponseEntity.ok(ApiResponse.ok("Supplier deactivated successfully", deactivated));
    }

    @PostMapping("/{id:[0-9]+}/block")
    public ResponseEntity<ApiResponse<SupplierDTO>> blockSupplier(@PathVariable Long id) {
        SupplierDTO blocked = supplierService.blockSupplier(id);
        return ResponseEntity.ok(ApiResponse.ok("Supplier blocked successfully", blocked));
    }

    @GetMapping("/{id:[0-9]+}/purchase-orders")
    public ResponseEntity<ApiResponse<List<PurchaseOrderDTO>>> getSupplierPurchaseOrders(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.getSupplierPurchaseOrders(id)));
    }
}
