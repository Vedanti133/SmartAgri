package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.CustomerTrackingDTO;
import com.smartagri.entity.Shipment;
import com.smartagri.repository.ShipmentRepository;
import com.smartagri.service.ShipmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class CustomerTrackingController {

    private final ShipmentService shipmentService;
    private final ShipmentRepository shipmentRepository;

    public CustomerTrackingController(ShipmentService shipmentService,
                                      ShipmentRepository shipmentRepository) {
        this.shipmentService = shipmentService;
        this.shipmentRepository = shipmentRepository;
    }

    @GetMapping("/api/orders/{orderId:[0-9]+}/tracking")
    public ResponseEntity<ApiResponse<CustomerTrackingDTO>> getOrderTracking(
            HttpServletRequest request,
            @PathVariable Long orderId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        CustomerTrackingDTO dto = shipmentService.getCustomerTracking(userId, role, orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order tracking retrieved successfully", dto));
    }

    @GetMapping("/api/customer/orders/{orderId:[0-9]+}/tracking")
    public ResponseEntity<ApiResponse<CustomerTrackingDTO>> getCustomerOrderTracking(
            HttpServletRequest request,
            @PathVariable Long orderId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        CustomerTrackingDTO dto = shipmentService.getCustomerTracking(userId, role, orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order tracking retrieved successfully", dto));
    }

    @GetMapping("/api/customer/shipments/{shipmentId:[0-9]+}/tracking")
    public ResponseEntity<ApiResponse<CustomerTrackingDTO>> getCustomerShipmentTracking(
            HttpServletRequest request,
            @PathVariable Long shipmentId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found with id: " + shipmentId));
        CustomerTrackingDTO dto = shipmentService.getCustomerTracking(userId, shipment.getOrder().getId());
        return ResponseEntity.ok(ApiResponse.ok("Shipment tracking retrieved successfully", dto));
    }
}
