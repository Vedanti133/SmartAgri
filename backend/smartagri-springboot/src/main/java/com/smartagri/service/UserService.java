package com.smartagri.service;

import com.smartagri.dto.UserResponseDTO;
import com.smartagri.entity.User;
import com.smartagri.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "ADMIN", "SALES_STAFF", "SUPPLIER", "SUPPLIERS", "DISTRIBUTOR", "DEALER", "RETAILER", "CUSTOMER"
    );

    public static final String ADMIN_USERNAME = "rahul";
    public static final String ADMIN_EMAIL = "rahul@gmail.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        String cleanUsername = user.getUsername().trim();

        if (userRepository.findByUsername(cleanUsername).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        String cleanEmail = user.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (user.getPassword() == null || user.getPassword().length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }

        String rawRole = user.getRole();
        String role;
        if (rawRole != null && "ADMIN".equalsIgnoreCase(rawRole.trim())) {
            // Only Rahul Kale (rahul / rahul@gmail.com) can ever hold the ADMIN role
            if (!ADMIN_USERNAME.equalsIgnoreCase(cleanUsername) || !ADMIN_EMAIL.equalsIgnoreCase(cleanEmail)) {
                throw new IllegalArgumentException("Registration with role ADMIN is restricted. Only designated administrator ('" + ADMIN_USERNAME + "' / '" + ADMIN_EMAIL + "') can have the ADMIN role.");
            }
            boolean adminAlreadyExists = userRepository.findAll().stream()
                    .anyMatch(u -> "ADMIN".equalsIgnoreCase(u.getRole()));
            if (adminAlreadyExists) {
                throw new IllegalArgumentException("An administrator account already exists. SmartAgri permits exactly one ADMIN account.");
            }
            role = "ADMIN";
        } else {
            // Cannot register with reserved admin credentials as non-admin
            if (ADMIN_USERNAME.equalsIgnoreCase(cleanUsername)) {
                throw new IllegalArgumentException("Username '" + ADMIN_USERNAME + "' is reserved for the system administrator.");
            }
            if (ADMIN_EMAIL.equalsIgnoreCase(cleanEmail)) {
                throw new IllegalArgumentException("Email '" + ADMIN_EMAIL + "' is reserved for the system administrator.");
            }
            if (rawRole == null || rawRole.trim().isEmpty()) {
                role = "CUSTOMER";
            } else {
                role = rawRole.trim().toUpperCase(Locale.ROOT);
                if ("SUPPLIERS".equals(role)) {
                    role = "SUPPLIER";
                }
                if (!ALLOWED_ROLES.contains(role) || "ADMIN".equalsIgnoreCase(role)) {
                    role = "CUSTOMER";
                }
            }
        }

        user.setUsername(cleanUsername);
        user.setEmail(cleanEmail);
        user.setName(user.getName() != null && !user.getName().trim().isEmpty() ? user.getName().trim() : cleanUsername);
        user.setRole(role);
        user.setActive(true);
        // BCrypt hash password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> login(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        String storedPassword = user.getPassword();

        // Check if stored password is BCrypt hashed
        boolean isBCrypt = storedPassword != null &&
                (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$"));

        if (isBCrypt) {
            if (passwordEncoder.matches(rawPassword, storedPassword)) {
                return Optional.of(user);
            }
        } else {
            // Legacy plaintext upgrade to BCrypt
            if (rawPassword.equals(storedPassword)) {
                log.info("Upgrading legacy plaintext password to BCrypt hash for user: {}", user.getUsername());
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserResponseDTO> getUser(Long id) {
        return userRepository.findById(id).map(this::toResponseDTO);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public UserResponseDTO toResponseDTO(User user) {
        if (user == null) return null;
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive()
        );
    }

    @Transactional
    public User updateUser(Long id, User updateRequest) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        // Validate role changes
        if (updateRequest.getRole() != null && !updateRequest.getRole().trim().isEmpty()) {
            String newRole = updateRequest.getRole().trim().toUpperCase(Locale.ROOT);
            if ("SUPPLIERS".equals(newRole)) {
                newRole = "SUPPLIER";
            }
            if ("ADMIN".equalsIgnoreCase(newRole)) {
                // TEST 3: Cannot change an existing non-admin user to ADMIN
                if (!ADMIN_USERNAME.equalsIgnoreCase(existing.getUsername()) || !ADMIN_EMAIL.equalsIgnoreCase(existing.getEmail())) {
                    throw new IllegalArgumentException("Cannot change role to ADMIN. Only " + ADMIN_USERNAME + " (" + ADMIN_EMAIL + ") can have the ADMIN role.");
                }
            } else {
                if ("ADMIN".equalsIgnoreCase(existing.getRole()) && ADMIN_USERNAME.equalsIgnoreCase(existing.getUsername())) {
                    throw new IllegalArgumentException("Cannot revoke the ADMIN role from the primary administrator account.");
                }
                if (!ALLOWED_ROLES.contains(newRole)) {
                    throw new IllegalArgumentException("Invalid role: " + newRole);
                }
                existing.setRole(newRole);
            }
        }

        if (updateRequest.getName() != null && !updateRequest.getName().trim().isEmpty()) {
            existing.setName(updateRequest.getName().trim());
        }

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().trim().isEmpty()) {
            String newEmail = updateRequest.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!newEmail.equalsIgnoreCase(existing.getEmail())) {
                if (ADMIN_EMAIL.equalsIgnoreCase(newEmail) && !ADMIN_USERNAME.equalsIgnoreCase(existing.getUsername())) {
                    throw new IllegalArgumentException("Email '" + ADMIN_EMAIL + "' is reserved for the administrator.");
                }
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new IllegalArgumentException("Email already exists");
                }
                existing.setEmail(newEmail);
            }
        }

        if (updateRequest.getPassword() != null && updateRequest.getPassword().length() >= 4) {
            existing.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }

        return userRepository.save(existing);
    }

    @Transactional
    public User updateUserRole(Long id, String newRole) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        if (newRole == null || newRole.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be empty");
        }
        String cleanRole = newRole.trim().toUpperCase(Locale.ROOT);
        if ("SUPPLIERS".equals(cleanRole)) {
            cleanRole = "SUPPLIER";
        }

        if ("ADMIN".equalsIgnoreCase(cleanRole)) {
            // TEST 3: Cannot change an existing non-admin user to ADMIN
            if (!ADMIN_USERNAME.equalsIgnoreCase(existing.getUsername()) || !ADMIN_EMAIL.equalsIgnoreCase(existing.getEmail())) {
                throw new IllegalArgumentException("Cannot promote user to ADMIN. Only " + ADMIN_USERNAME + " (" + ADMIN_EMAIL + ") can have the ADMIN role.");
            }
        } else {
            if ("ADMIN".equalsIgnoreCase(existing.getRole()) && ADMIN_USERNAME.equalsIgnoreCase(existing.getUsername())) {
                throw new IllegalArgumentException("Cannot revoke the ADMIN role from the primary administrator account.");
            }
            if (!ALLOWED_ROLES.contains(cleanRole)) {
                throw new IllegalArgumentException("Invalid role: " + cleanRole);
            }
            existing.setRole(cleanRole);
        }

        return userRepository.save(existing);
    }
}
