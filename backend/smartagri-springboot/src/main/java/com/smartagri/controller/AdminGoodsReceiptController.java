package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.entity.GoodsReceiptStatus;
import com.smartagri.service.GoodsReceiptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/goods-receipts")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminGoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    public AdminGoodsReceiptController(GoodsReceiptService goodsReceiptService) {
        this.goodsReceiptService = goodsReceiptService;
    }

    private String getAuthenticatedUsername(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        return (username != null && !username.isBlank()) ? username : "admin";
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoodsReceiptDTO>>> getAllGoodsReceipts(
            @RequestParam(required = false) Long poId,
            @RequestParam(required = false) GoodsReceiptStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(goodsReceiptService.getAllGoodsReceipts(poId, status, search)));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<GoodsReceiptDTO>> getGoodsReceiptById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(goodsReceiptService.getGoodsReceiptById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GoodsReceiptDTO>> createGoodsReceipt(
            HttpServletRequest request,
            @Valid @RequestBody CreateGoodsReceiptRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        GoodsReceiptDTO created = goodsReceiptService.createGoodsReceipt(body, adminUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Goods Receipt #" + created.getGrnNumber() + " created successfully", created));
    }

    @PostMapping("/{id:[0-9]+}/inspect")
    public ResponseEntity<ApiResponse<GoodsReceiptDTO>> inspectGoodsReceipt(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody InspectGoodsReceiptRequest body) {
        String adminUser = getAuthenticatedUsername(request);
        GoodsReceiptDTO inspected = goodsReceiptService.inspectGoodsReceipt(id, body, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Goods Receipt #" + inspected.getGrnNumber() + " inspected successfully", inspected));
    }

    @PostMapping("/{id:[0-9]+}/accept")
    public ResponseEntity<ApiResponse<GoodsReceiptDTO>> acceptGoodsReceipt(
            HttpServletRequest request,
            @PathVariable Long id) {
        String adminUser = getAuthenticatedUsername(request);
        GoodsReceiptDTO accepted = goodsReceiptService.acceptGoodsReceipt(id, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Goods Receipt #" + accepted.getGrnNumber() + " accepted and stock integrated into inventory", accepted));
    }

    @PostMapping("/{id:[0-9]+}/reject")
    public ResponseEntity<ApiResponse<GoodsReceiptDTO>> rejectGoodsReceipt(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String adminUser = getAuthenticatedUsername(request);
        String reason = (body != null) ? body.get("reason") : "Quality rejection";
        GoodsReceiptDTO rejected = goodsReceiptService.rejectGoodsReceipt(id, reason, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("Goods Receipt #" + rejected.getGrnNumber() + " rejected", rejected));
    }
}
