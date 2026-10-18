package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.service.PickListService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/picklists")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminPickListController {

    private final PickListService pickListService;

    public AdminPickListController(PickListService pickListService) {
        this.pickListService = pickListService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PickListDTO>>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long orderId) {
        List<PickListDTO> list = pickListService.getPickLists(status, warehouseId, orderId);
        return ResponseEntity.ok(ApiResponse.ok("Pick lists retrieved successfully", list));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<PickListDTO>> getOne(@PathVariable Long id) {
        PickListDTO dto = pickListService.getPickListById(id);
        return ResponseEntity.ok(ApiResponse.ok("Pick list retrieved successfully", dto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PickListDTO>> create(
            HttpServletRequest request,
            @Valid @RequestBody CreatePickListRequest createReq) {
        String username = (String) request.getAttribute("currentUsername");
        PickListDTO created = pickListService.createPickList(createReq, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Pick list created successfully", created));
    }

    @PutMapping("/{id:[0-9]+}/assign")
    public ResponseEntity<ApiResponse<PickListDTO>> assignPicker(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String assignedUser) {
        String picker = assignedUser;
        if ((picker == null || picker.isBlank()) && body != null && body.containsKey("assignedUser")) {
            picker = body.get("assignedUser");
        }
        PickListDTO updated = pickListService.assignPicker(id, picker);
        return ResponseEntity.ok(ApiResponse.ok("Picker assigned successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/start")
    public ResponseEntity<ApiResponse<PickListDTO>> startPicking(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String picker = (String) request.getAttribute("currentUsername");
        if (body != null && body.containsKey("assignedUser")) {
            picker = body.get("assignedUser");
        }
        PickListDTO updated = pickListService.startPicking(id, picker);
        return ResponseEntity.ok(ApiResponse.ok("Picking started successfully", updated));
    }

    @PostMapping({ "/{id:[0-9]+}/pick", "/{id:[0-9]+}/confirm" })
    public ResponseEntity<ApiResponse<PickListDTO>> confirmPick(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmPickRequest confirmReq) {
        String username = (String) request.getAttribute("currentUsername");
        PickListDTO updated = pickListService.confirmPick(id, confirmReq, username);
        return ResponseEntity.ok(ApiResponse.ok("Pick confirmed successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/complete")
    public ResponseEntity<ApiResponse<PickListDTO>> completePick(
            HttpServletRequest request,
            @PathVariable Long id) {
        String username = (String) request.getAttribute("currentUsername");
        PickListDTO updated = pickListService.completePickList(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Pick list completed successfully", updated));
    }

    @PostMapping("/{id:[0-9]+}/cancel")
    public ResponseEntity<ApiResponse<PickListDTO>> cancelPick(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String reason) {
        String username = (String) request.getAttribute("currentUsername");
        String finalReason = reason;
        if ((finalReason == null || finalReason.isBlank()) && body != null && body.containsKey("reason")) {
            finalReason = body.get("reason");
        }
        PickListDTO updated = pickListService.cancelPickList(id, finalReason, username);
        return ResponseEntity.ok(ApiResponse.ok("Pick list cancelled successfully", updated));
    }
}
