package com.smartagri.service;

import com.smartagri.dto.CustomerProfileDTO;
import com.smartagri.entity.CustomerProfile;
import com.smartagri.entity.User;
import com.smartagri.repository.CustomerProfileRepository;
import com.smartagri.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerProfileService {

    private final CustomerProfileRepository profileRepository;
    private final UserRepository userRepository;

    public CustomerProfileService(CustomerProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CustomerProfileDTO getProfile(Long userId) {
        CustomerProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
                    CustomerProfile newProfile = new CustomerProfile(user, user.getName(), "", user.getEmail());
                    return profileRepository.save(newProfile);
                });

        return toDTO(profile);
    }

    @Transactional
    public CustomerProfileDTO updateProfile(Long userId, CustomerProfileDTO dto) {
        CustomerProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
                    return new CustomerProfile(user, user.getName(), "", user.getEmail());
                });

        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            profile.setFullName(dto.getFullName().trim());
        }
        if (dto.getPhone() != null) {
            profile.setPhone(dto.getPhone().trim());
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            profile.setEmail(dto.getEmail().trim());
        }

        CustomerProfile saved = profileRepository.save(profile);
        return toDTO(saved);
    }

    private CustomerProfileDTO toDTO(CustomerProfile p) {
        User u = p.getUser();
        return new CustomerProfileDTO(
                p.getId(),
                u != null ? u.getId() : null,
                u != null ? u.getUsername() : "",
                p.getFullName(),
                p.getPhone(),
                p.getEmail(),
                u != null ? u.getRole() : ""
        );
    }
}
