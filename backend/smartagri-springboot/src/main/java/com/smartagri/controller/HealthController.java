package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/health", "/health"})
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "SmartAgri Enterprise Platform");
        health.put("version", "1.0.0");
        health.put("systemTime", System.currentTimeMillis());
        return ResponseEntity.ok(ApiResponse.ok("SmartAgri Backend is operational", health));
    }
}
