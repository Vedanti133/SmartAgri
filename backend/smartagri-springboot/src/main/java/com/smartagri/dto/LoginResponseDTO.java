package com.smartagri.dto;

public class LoginResponseDTO {
    private String token;
    private Long id;
    private String username;
    private String name;
    private String email;
    private String role;
    private UserResponseDTO user;

    public LoginResponseDTO() {}

    public LoginResponseDTO(String token, UserResponseDTO user) {
        this.token = token;
        this.user = user;
        if (user != null) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.name = user.getName();
            this.email = user.getEmail();
            this.role = user.getRole();
        }
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public UserResponseDTO getUser() { return user; }
    public void setUser(UserResponseDTO user) {
        this.user = user;
        if (user != null) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.name = user.getName();
            this.email = user.getEmail();
            this.role = user.getRole();
        }
    }
}
