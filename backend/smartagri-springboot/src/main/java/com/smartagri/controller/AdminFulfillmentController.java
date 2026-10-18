package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.service.FulfillmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/fulfillment")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminFulfillmentController {

    private final FulfillmentService fulfillmentService;

    public AdminFulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping({"", "/orders"})
    public ResponseEntity<ApiResponse<List<OrderFulfillmentDetailDTO>>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String search) {
        List<OrderFulfillmentDetailDTO> list = fulfillmentService.getAllFulfillments(status, warehouseId, search);
        return ResponseEntity.ok(ApiResponse.ok("Fulfillments retrieved successfully", list));
    }

    @GetMapping({"/{orderId:[0-9]+}", "/orders/{orderId:[0-9]+}"})
    public ResponseEntity<ApiResponse<OrderFulfillmentDetailDTO>> getOne(@PathVariable Long orderId) {
        OrderFulfillmentDetailDTO detail = fulfillmentService.getFulfillmentDetail(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Fulfillment detail retrieved successfully", detail));
    }

    @PostMapping({"/{orderId:[0-9]+}/allocate", "/orders/{orderId:[0-9]+}/allocate"})
    public ResponseEntity<ApiResponse<List<FulfillmentAllocationDTO>>> allocateOrder(
            HttpServletRequest request,
            @PathVariable Long orderId) {
        String username = (String) request.getAttribute("currentUsername");
        List<FulfillmentAllocationDTO> allocations = fulfillmentService.allocateOrder(orderId, username);
        return ResponseEntity.ok(ApiResponse.ok("Order allocated successfully", allocations));
    }

    @GetMapping({"/{orderId:[0-9]+}/allocations", "/orders/{orderId:[0-9]+}/allocations"})
    public ResponseEntity<ApiResponse<List<FulfillmentAllocationDTO>>> getAllocations(@PathVariable Long orderId) {
        List<FulfillmentAllocationDTO> allocations = fulfillmentService.getAllocationsForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Allocations retrieved successfully", allocations));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<FulfillmentDashboardDTO>> getDashboard() {
        FulfillmentDashboardDTO dashboard = fulfillmentService.getDashboard();
        return ResponseEntity.ok(ApiResponse.ok("Fulfillment dashboard metrics retrieved successfully", dashboard));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<FulfillmentHealthDTO>> checkHealth() {
        FulfillmentHealthDTO health = fulfillmentService.checkFulfillmentHealth();
        return ResponseEntity.ok(ApiResponse.ok("Fulfillment health check completed", health));
    }
}
