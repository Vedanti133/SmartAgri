package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.UserResponseDTO;
import com.smartagri.entity.User;
import com.smartagri.security.TokenService;
import com.smartagri.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;

    public AuthController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User saved = userService.register(user);
            String token = tokenService.generateToken(saved.getId(), saved.getUsername(), saved.getRole());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", saved.getId());
            data.put("name", saved.getName());
            data.put("username", saved.getUsername());
            data.put("email", saved.getEmail());
            data.put("role", saved.getRole());
            data.put("token", token);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "message", "Registration successful", "data", data));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");

        return userService.login(username, password)
                .map(user -> {
                    String token = tokenService.generateToken(user.getId(), user.getUsername(), user.getRole());

                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("id", user.getId());
                    data.put("username", user.getUsername());
                    data.put("name", user.getName());
                    data.put("email", user.getEmail());
                    data.put("role", user.getRole());
                    data.put("token", token);

                    return ResponseEntity.ok(
                            Map.of("success", true, "message", "Login successful", "data", data));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Invalid username or password")));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Session expired or invalid"));
        }

        return userService.getUser(userId)
                .map(u -> ResponseEntity.ok(ApiResponse.ok(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found")));
    }
}
