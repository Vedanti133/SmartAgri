package com.smartagri.security;

import java.util.Locale;

public class AuthenticatedUser {
    private final Long userId;
    private final String username;
    private final String role;

    public AuthenticatedUser(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = (role != null) ? role.trim().toUpperCase(Locale.ROOT) : "CUSTOMER";
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean hasRole(String targetRole) {
        if (this.role == null || targetRole == null) return false;
        String r = this.role.toUpperCase(Locale.ROOT);
        String t = targetRole.toUpperCase(Locale.ROOT);
        if (r.equals(t)) return true;
        if ((r.equals("SUPPLIER") && t.equals("SUPPLIERS")) || (r.equals("SUPPLIERS") && t.equals("SUPPLIER"))) return true;
        return false;
    }

    public boolean hasAnyRole(String... targetRoles) {
        if (targetRoles == null) return false;
        for (String targetRole : targetRoles) {
            if (hasRole(targetRole)) return true;
        }
        return false;
    }
}

