package com.smartagri.controller;

import com.smartagri.dto.AddressDTO;
import com.smartagri.dto.ApiResponse;
import com.smartagri.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressDTO>>> getAll(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.ok(addressService.getAddresses(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressDTO>> getOne(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.ok(addressService.getAddress(userId, id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressDTO>> create(HttpServletRequest request,
                                                           @Valid @RequestBody AddressDTO dto) {
        Long userId = (Long) request.getAttribute("currentUserId");
        AddressDTO created = addressService.createAddress(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Address added successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressDTO>> update(HttpServletRequest request,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody AddressDTO dto) {
        Long userId = (Long) request.getAttribute("currentUserId");
        AddressDTO updated = addressService.updateAddress(userId, id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Address updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("currentUserId");
        addressService.deleteAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted successfully", null));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressDTO>> setDefault(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("currentUserId");
        AddressDTO updated = addressService.setDefaultAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Default address updated", updated));
    }
}
