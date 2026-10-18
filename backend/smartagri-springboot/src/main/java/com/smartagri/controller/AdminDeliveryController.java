package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.AssignDeliveryRequest;
import com.smartagri.dto.DeliveryDTO;
import com.smartagri.dto.UpdateDeliveryStatusRequest;
import com.smartagri.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/deliveries")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminDeliveryController {

    private final DeliveryService deliveryService;

    public AdminDeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryDTO>>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        List<DeliveryDTO> deliveries = deliveryService.getAllDeliveriesAdmin(status, search);
        return ResponseEntity.ok(ApiResponse.ok("Deliveries retrieved successfully", deliveries));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryDTO>> getOne(@PathVariable Long id) {
        DeliveryDTO dto = deliveryService.getDeliveryAdmin(id);
        return ResponseEntity.ok(ApiResponse.ok("Delivery retrieved successfully", dto));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<DeliveryDTO>> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignDeliveryRequest request) {
        DeliveryDTO updated = deliveryService.assignDeliveryPartner(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Delivery partner assigned successfully", updated));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DeliveryDTO>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        DeliveryDTO updated = deliveryService.updateDeliveryStatus(id, request.getDeliveryStatus(), request.getNotes());
        return ResponseEntity.ok(ApiResponse.ok("Delivery status updated successfully", updated));
    }
}
