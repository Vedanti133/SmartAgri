package com.smartagri.controller;

import com.smartagri.dto.*;
import com.smartagri.service.SupportTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/support/tickets")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class CustomerSupportTicketController {

    private final SupportTicketService ticketService;

    public CustomerSupportTicketController(SupportTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupportTicketDTO>> createTicket(
            HttpServletRequest request,
            @Valid @RequestBody CreateSupportTicketRequest body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        SupportTicketDTO res = ticketService.createTicket(userId, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Support ticket submitted! Ticket #" + res.getTicketNumber(), res));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportTicketDTO>>> getCustomerTickets(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getCustomerTickets(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> getTicket(
            HttpServletRequest request,
            @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getCustomerTicket(userId, id)));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> addMessage(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody AddTicketMessageRequest body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        SupportTicketDTO res = ticketService.addMessage(userId, id, body, false);
        return ResponseEntity.ok(ApiResponse.ok("Message sent", res));
    }
}
