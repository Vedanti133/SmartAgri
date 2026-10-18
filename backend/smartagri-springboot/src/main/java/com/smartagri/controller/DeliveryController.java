package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.DeliveryDTO;
import com.smartagri.service.DeliveryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{id}/delivery")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DeliveryDTO>> getDelivery(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("currentUserId");
        DeliveryDTO dto = deliveryService.getDeliveryForOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Delivery information retrieved successfully", dto));
    }
}
