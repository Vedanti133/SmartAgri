package com.smartagri.dto;

public class CustomerProfileDTO {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String phone;
    private String email;
    private String role;

    public CustomerProfileDTO() {}

    public CustomerProfileDTO(Long id, Long userId, String username, String fullName, String phone, String email, String role) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
