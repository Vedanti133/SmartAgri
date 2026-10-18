package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.entity.ReturnShipment;
import com.smartagri.enums.ReturnShipmentStatus;
import com.smartagri.service.RefundService;
import com.smartagri.service.ReturnService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class CustomerReturnController {

    private final ReturnService returnService;
    private final RefundService refundService;

    public CustomerReturnController(ReturnService returnService, RefundService refundService) {
        this.returnService = returnService;
        this.refundService = refundService;
    }

    @PostMapping("/api/customer/returns")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> createReturn(HttpServletRequest request,
                                                                      @Valid @RequestBody CreateReturnRequest body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        ReturnRequestDTO res = returnService.createReturn(userId, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Return request submitted successfully! Return Number: " + res.getReturnNumber(), res));
    }

    @PostMapping("/api/orders/{orderId}/returns")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> createOrderReturn(HttpServletRequest request,
                                                                          @PathVariable Long orderId,
                                                                          @Valid @RequestBody CreateReturnRequest body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        body.setOrderId(orderId);
        ReturnRequestDTO res = returnService.createReturn(userId, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Return request submitted successfully! Return Number: " + res.getReturnNumber(), res));
    }

    @GetMapping("/api/customer/returns")
    public ResponseEntity<ApiResponse<List<ReturnRequestDTO>>> getCustomerReturns(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.ok(returnService.getCustomerReturns(userId)));
    }

    @GetMapping("/api/customer/returns/{id}")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> getReturnById(HttpServletRequest request,
                                                                       @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("currentUserId");
        ReturnRequestDTO res = returnService.getReturnById(id);
        if (!res.getCustomerId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied: You do not own this return"));
        }
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @RequestMapping(value = "/api/customer/returns/{id}/cancel", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> cancelReturn(HttpServletRequest request,
                                                                      @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("currentUserId");
        ReturnRequestDTO res = returnService.cancelReturn(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Return request cancelled successfully", res));
    }

    @GetMapping("/api/customer/returns/{id}/tracking")
    public ResponseEntity<ApiResponse<ReturnShipmentDTO>> getReturnTracking(HttpServletRequest request,
                                                                            @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("currentUserId");
        ReturnRequestDTO res = returnService.getReturnById(id);
        if (!res.getCustomerId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied: You do not own this return"));
        }
        return ResponseEntity.ok(ApiResponse.ok(res.getShipment()));
    }

    @PostMapping("/api/customer/refunds/{id}/bank-details")
    public ResponseEntity<ApiResponse<RefundDTO>> updateBankDetails(HttpServletRequest request,
                                                                    @PathVariable Long id,
                                                                    @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        String details = body.get("bankAccountDetails");
        RefundDTO res = refundService.updateBankDetails(userId, id, details);
        return ResponseEntity.ok(ApiResponse.ok("Bank account details updated successfully", res));
    }

    @GetMapping("/api/customer/refunds")
    public ResponseEntity<ApiResponse<List<RefundDTO>>> getCustomerRefunds(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        List<ReturnRequestDTO> returns = returnService.getCustomerReturns(userId);
        List<RefundDTO> refunds = returns.stream()
                .filter(r -> r.getRefund() != null)
                .map(ReturnRequestDTO::getRefund)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(refunds));
    }

    @GetMapping("/api/customer/replacements")
    public ResponseEntity<ApiResponse<List<ReplacementOrderDTO>>> getCustomerReplacements(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        List<ReturnRequestDTO> returns = returnService.getCustomerReturns(userId);
        List<ReplacementOrderDTO> replacements = returns.stream()
                .filter(r -> r.getReplacement() != null)
                .map(ReturnRequestDTO::getReplacement)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(replacements));
    }
}
