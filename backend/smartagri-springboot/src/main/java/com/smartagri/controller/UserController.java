package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.UserResponseDTO;
import com.smartagri.entity.User;
import com.smartagri.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(@PathVariable Long id) {
        return userService.getUser(id)
                .map(u -> ResponseEntity.ok(ApiResponse.ok(u)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("User not found")));
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User saved = userService.register(user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("User created successfully", userService.toResponseDTO(saved)));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User updated = userService.updateUser(id, user);
            return ResponseEntity.ok(ApiResponse.ok("User updated successfully", userService.toResponseDTO(updated)));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String role = body.getOrDefault("role", "");
            User updated = userService.updateUserRole(id, role);
            return ResponseEntity.ok(ApiResponse.ok("User role updated successfully", userService.toResponseDTO(updated)));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }
}
