package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.CreateDeliveryPartnerRequest;
import com.smartagri.dto.DeliveryPartnerDTO;
import com.smartagri.service.DeliveryPartnerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/delivery-partners")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminDeliveryPartnerController {

    private final DeliveryPartnerService deliveryPartnerService;

    public AdminDeliveryPartnerController(DeliveryPartnerService deliveryPartnerService) {
        this.deliveryPartnerService = deliveryPartnerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryPartnerDTO>>> getAll(
            @RequestParam(required = false) String status) {
        List<DeliveryPartnerDTO> list = deliveryPartnerService.getAllPartners(status);
        return ResponseEntity.ok(ApiResponse.ok("Delivery partners retrieved successfully", list));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<DeliveryPartnerDTO>> getOne(@PathVariable Long id) {
        DeliveryPartnerDTO dto = deliveryPartnerService.getPartnerById(id);
        return ResponseEntity.ok(ApiResponse.ok("Delivery partner retrieved successfully", dto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryPartnerDTO>> create(
            @Valid @RequestBody CreateDeliveryPartnerRequest request) {
        DeliveryPartnerDTO created = deliveryPartnerService.createPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Delivery partner created successfully", created));
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<DeliveryPartnerDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateDeliveryPartnerRequest request) {
        DeliveryPartnerDTO updated = deliveryPartnerService.updatePartner(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Delivery partner updated successfully", updated));
    }

    @RequestMapping(value = "/{id:[0-9]+}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<DeliveryPartnerDTO>> updateStatus(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String status) {
        String finalStatus = status;
        if ((finalStatus == null || finalStatus.isBlank()) && body != null && body.containsKey("status")) {
            finalStatus = body.get("status");
        }
        DeliveryPartnerDTO updated = deliveryPartnerService.updateStatus(id, finalStatus);
        return ResponseEntity.ok(ApiResponse.ok("Delivery partner status updated successfully", updated));
    }
}
