package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.OrderDTO;
import com.smartagri.dto.OrderMetricsDTO;
import com.smartagri.dto.UpdateOrderStatusRequest;
import com.smartagri.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getAllOrders(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrdersAdmin(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderAdmin(id)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateStatus(@PathVariable Long id,
                                                              @Valid @RequestBody UpdateOrderStatusRequest body) {
        OrderDTO updated = orderService.updateOrderStatusAdmin(id, body.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Order status updated to " + updated.getOrderStatus(), updated));
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<OrderMetricsDTO>> getOrderMetrics() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderMetricsAdmin()));
    }

    @GetMapping("/fulfillment-metrics")
    public ResponseEntity<ApiResponse<com.smartagri.dto.FulfillmentMetricsDTO>> getFulfillmentMetrics() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getFulfillmentMetricsAdmin()));
    }
}
