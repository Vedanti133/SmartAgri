package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.InvoiceDTO;
import com.smartagri.security.AuthenticatedUser;
import com.smartagri.service.InvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{id}/invoice")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<InvoiceDTO>> getInvoice(HttpServletRequest request, @PathVariable Long id) {
        AuthenticatedUser currentUser = (AuthenticatedUser) request.getAttribute("currentUser");
        Long userId = (Long) request.getAttribute("currentUserId");

        InvoiceDTO dto;
        if (currentUser != null && currentUser.isAdmin()) {
            dto = invoiceService.getAdminInvoice(id);
        } else {
            dto = invoiceService.getCustomerInvoice(userId, id);
        }

        return ResponseEntity.ok(ApiResponse.ok("Invoice details retrieved successfully", dto));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(HttpServletRequest request, @PathVariable Long id) {
        AuthenticatedUser currentUser = (AuthenticatedUser) request.getAttribute("currentUser");
        Long userId = (Long) request.getAttribute("currentUserId");
        boolean isAdmin = currentUser != null && currentUser.isAdmin();

        byte[] pdfBytes = invoiceService.generateInvoicePdf(userId, id, isAdmin);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "SmartAgri-Invoice-Order-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
