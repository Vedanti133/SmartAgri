package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.enums.ReturnShipmentStatus;
import com.smartagri.enums.ReturnStatus;
import com.smartagri.service.ReturnService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/returns")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminReturnController {

    private final ReturnService returnService;

    public AdminReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReturnRequestDTO>>> getAllReturns(
            @RequestParam(required = false) ReturnStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(returnService.getAllReturns(status)));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ReturnDashboardDTO>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(returnService.getDashboard()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> getReturnById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(returnService.getReturnById(id)));
    }

    @RequestMapping(value = "/{id}/approve", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> approveReturn(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveReturnRequest body) {
        ReturnRequestDTO res = returnService.approveReturn(id, body != null ? body : new ApproveReturnRequest());
        return ResponseEntity.ok(ApiResponse.ok("Return request #" + res.getReturnNumber() + " approved successfully", res));
    }

    @RequestMapping(value = "/{id}/reject", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> rejectReturn(
            @PathVariable Long id,
            @RequestBody(required = false) RejectReturnRequest body) {
        ReturnRequestDTO res = returnService.rejectReturn(id, body != null ? body : new RejectReturnRequest());
        return ResponseEntity.ok(ApiResponse.ok("Return request #" + res.getReturnNumber() + " rejected", res));
    }

    @PostMapping("/{id}/pickup")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> schedulePickup(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleReturnPickupRequest body) {
        ReturnRequestDTO res = returnService.schedulePickup(id, body);
        return ResponseEntity.ok(ApiResponse.ok("Pickup scheduled successfully for return #" + res.getReturnNumber(), res));
    }

    @PostMapping("/{id}/shipment-status")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> updateShipmentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ReturnShipmentStatus status = ReturnShipmentStatus.valueOf(body.get("status"));
        String location = body.get("location");
        String notes = body.get("notes");
        ReturnRequestDTO res = returnService.updateShipmentStatus(id, status, location, notes);
        return ResponseEntity.ok(ApiResponse.ok("Shipment status updated to " + status, res));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> receiveReturn(
            @PathVariable Long id,
            @RequestBody(required = false) ReceiveReturnRequest body) {
        ReturnRequestDTO res = returnService.receiveReturn(id, body != null ? body : new ReceiveReturnRequest());
        return ResponseEntity.ok(ApiResponse.ok("Return package #" + res.getReturnNumber() + " received at warehouse", res));
    }

    @PostMapping("/{id}/inspect")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> inspectReturn(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody InspectReturnRequest body) {
        String inspector = (String) request.getAttribute("currentUsername");
        ReturnRequestDTO res = returnService.inspectReturn(id, body, inspector);
        return ResponseEntity.ok(ApiResponse.ok("Inspection completed for return #" + res.getReturnNumber(), res));
    }
}
