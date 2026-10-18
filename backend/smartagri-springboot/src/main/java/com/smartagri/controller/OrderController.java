package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.CreateOrderRequest;
import com.smartagri.dto.OrderDTO;
import com.smartagri.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(HttpServletRequest request,
                                                             @Valid @RequestBody CreateOrderRequest body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        OrderDTO order = orderService.createOrder(userId, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed successfully! Order Number: " + order.getOrderNumber(), order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getCustomerOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.ok(orderService.getCustomerOrders(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getCustomerOrder(HttpServletRequest request,
                                                                  @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.ok(orderService.getCustomerOrder(userId, id)));
    }

    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(HttpServletRequest request,
                                                             @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("currentUserId");
        OrderDTO cancelled = orderService.cancelOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Order #" + cancelled.getOrderNumber() + " cancelled successfully", cancelled));
    }
}
