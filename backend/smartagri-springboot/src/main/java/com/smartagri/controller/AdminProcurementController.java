package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.ProcurementDashboardDTO;
import com.smartagri.service.ProcurementDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/procurement")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AdminProcurementController {

    private final ProcurementDashboardService procurementDashboardService;

    public AdminProcurementController(ProcurementDashboardService procurementDashboardService) {
        this.procurementDashboardService = procurementDashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ProcurementDashboardDTO>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(procurementDashboardService.getDashboardMetrics()));
    }
}
