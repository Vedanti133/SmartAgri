package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.CustomerProfileDTO;
import com.smartagri.service.CustomerProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class ProfileController {

    private final CustomerProfileService profileService;

    public ProfileController(CustomerProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CustomerProfileDTO>> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        CustomerProfileDTO profile = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CustomerProfileDTO>> updateProfile(HttpServletRequest request,
                                                                         @RequestBody CustomerProfileDTO dto) {
        Long userId = (Long) request.getAttribute("currentUserId");
        CustomerProfileDTO updated = profileService.updateProfile(userId, dto);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }
}
