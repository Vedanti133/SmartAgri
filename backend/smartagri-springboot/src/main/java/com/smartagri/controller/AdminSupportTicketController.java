package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.enums.TicketStatus;
import com.smartagri.service.SupportTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/support/tickets")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminSupportTicketController {

    private final SupportTicketService ticketService;

    public AdminSupportTicketController(SupportTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportTicketDTO>>> getAllTickets(
            @RequestParam(required = false) TicketStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getAllTickets(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getTicketById(id)));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> addStaffMessage(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody AddTicketMessageRequest body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        SupportTicketDTO res = ticketService.addMessage(userId, id, body, true);
        return ResponseEntity.ok(ApiResponse.ok("Reply sent", res));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateTicketStatusRequest body) {
        SupportTicketDTO res = ticketService.updateStatus(id, body);
        return ResponseEntity.ok(ApiResponse.ok("Ticket updated", res));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> assignTicket(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        UpdateTicketStatusRequest req = new UpdateTicketStatusRequest();
        req.setAssignedTo(body.get("assignedTo"));
        req.setStatus(TicketStatus.IN_PROGRESS);
        SupportTicketDTO res = ticketService.updateStatus(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Ticket assigned to " + req.getAssignedTo(), res));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> resolveTicket(@PathVariable Long id) {
        UpdateTicketStatusRequest req = new UpdateTicketStatusRequest();
        req.setStatus(TicketStatus.RESOLVED);
        SupportTicketDTO res = ticketService.updateStatus(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Ticket marked as RESOLVED", res));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> closeTicket(@PathVariable Long id) {
        UpdateTicketStatusRequest req = new UpdateTicketStatusRequest();
        req.setStatus(TicketStatus.CLOSED);
        SupportTicketDTO res = ticketService.updateStatus(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Ticket CLOSED", res));
    }
}
