package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.service.ShipmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/shipments")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminShipmentController {

    private final ShipmentService shipmentService;

    public AdminShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShipmentDTO>>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String courier,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long warehouseId) {
        List<ShipmentDTO> list = shipmentService.getShipments(status, courier, search, warehouseId);
        return ResponseEntity.ok(ApiResponse.ok("Shipments retrieved successfully", list));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<ShipmentDTO>> getOne(@PathVariable Long id) {
        ShipmentDTO dto = shipmentService.getShipmentById(id);
        return ResponseEntity.ok(ApiResponse.ok("Shipment retrieved successfully", dto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentDTO>> create(
            HttpServletRequest request,
            @Valid @RequestBody CreateShipmentRequest createReq) {
        String username = (String) request.getAttribute("currentUsername");
        ShipmentDTO created = shipmentService.createShipment(createReq, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Shipment created successfully", created));
    }

    @PostMapping("/{id:[0-9]+}/dispatch")
    public ResponseEntity<ApiResponse<ShipmentDTO>> dispatch(
            HttpServletRequest request,
            @PathVariable Long id) {
        String username = (String) request.getAttribute("currentUsername");
        ShipmentDTO dispatched = shipmentService.dispatchShipment(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Shipment dispatched successfully", dispatched));
    }

    @PostMapping({"/{id:[0-9]+}/tracking", "/{id:[0-9]+}/events"})
    public ResponseEntity<ApiResponse<ShipmentDTO>> addTracking(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody AddTrackingEventRequest trackingReq) {
        String username = (String) request.getAttribute("currentUsername");
        ShipmentDTO updated = shipmentService.addTrackingEvent(id, trackingReq, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tracking event added successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/out-for-delivery")
    public ResponseEntity<ApiResponse<ShipmentDTO>> markOutForDelivery(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String notes) {
        String username = (String) request.getAttribute("currentUsername");
        String finalNotes = notes;
        if ((finalNotes == null || finalNotes.isBlank()) && body != null && body.containsKey("notes")) {
            finalNotes = body.get("notes");
        }
        ShipmentDTO updated = shipmentService.markOutForDelivery(id, finalNotes, username);
        return ResponseEntity.ok(ApiResponse.ok("Shipment marked as OUT_FOR_DELIVERY", updated));
    }

    @PostMapping("/{id:[0-9]+}/deliver")
    public ResponseEntity<ApiResponse<ShipmentDTO>> markDelivered(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) DeliveryProofRequest proof) {
        String username = (String) request.getAttribute("currentUsername");
        ShipmentDTO updated = shipmentService.markDelivered(id, proof, username);
        return ResponseEntity.ok(ApiResponse.ok("Shipment delivered successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/fail")
    public ResponseEntity<ApiResponse<ShipmentDTO>> markFailed(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody DeliveryFailureRequest failReq) {
        String username = (String) request.getAttribute("currentUsername");
        ShipmentDTO updated = shipmentService.recordDeliveryFailure(id, failReq, username);
        return ResponseEntity.ok(ApiResponse.ok("Delivery failure recorded successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/rto")
    public ResponseEntity<ApiResponse<ShipmentDTO>> initiateRTO(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String reason) {
        String username = (String) request.getAttribute("currentUsername");
        String finalReason = reason;
        if ((finalReason == null || finalReason.isBlank()) && body != null && body.containsKey("reason")) {
            finalReason = body.get("reason");
        }
        ShipmentDTO updated = shipmentService.initiateRTO(id, finalReason, username);
        return ResponseEntity.ok(ApiResponse.ok("Return to origin initiated successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/receive-return")
    public ResponseEntity<ApiResponse<ShipmentDTO>> receiveReturn(
            HttpServletRequest request,
            @PathVariable Long id) {
        String username = (String) request.getAttribute("currentUsername");
        ShipmentDTO updated = shipmentService.receiveReturnedStock(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Returned shipment stock received and restored into warehouse", updated));
    }
}
