package com.smartagri.service;

import com.smartagri.dto.AddressDTO;
import com.smartagri.entity.Address;
import com.smartagri.entity.User;
import com.smartagri.repository.AddressRepository;
import com.smartagri.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AddressService {

    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    public List<AddressDTO> getAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AddressDTO getAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found with id: " + addressId));
        return toDTO(address);
    }

    @Transactional
    public AddressDTO createAddress(Long userId, AddressDTO dto) {
        validateAddress(dto);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        long existingCount = addressRepository.countByUserId(userId);
        boolean makeDefault = dto.isDefault() || existingCount == 0;

        if (makeDefault) {
            unsetDefaultAddress(userId);
        }

        Address address = new Address();
        address.setUser(user);
        address.setFullName(dto.getFullName().trim());
        address.setPhone(dto.getPhone().trim());
        address.setAddressLine1(dto.getAddressLine1().trim());
        address.setAddressLine2(dto.getAddressLine2() != null ? dto.getAddressLine2().trim() : null);
        address.setCity(dto.getCity().trim());
        address.setDistrict(dto.getDistrict() != null ? dto.getDistrict().trim() : null);
        address.setState(dto.getState().trim());
        address.setPincode(dto.getPincode().trim());
        address.setLandmark(dto.getLandmark() != null ? dto.getLandmark().trim() : null);
        address.setAddressType(dto.getAddressType() != null && !dto.getAddressType().isBlank() ? dto.getAddressType().trim().toUpperCase() : "HOME");
        address.setDefault(makeDefault);

        Address saved = addressRepository.save(address);
        return toDTO(saved);
    }

    @Transactional
    public AddressDTO updateAddress(Long userId, Long addressId, AddressDTO dto) {
        validateAddress(dto);

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found with id: " + addressId));

        if (dto.isDefault() && !address.isDefault()) {
            unsetDefaultAddress(userId);
            address.setDefault(true);
        }

        address.setFullName(dto.getFullName().trim());
        address.setPhone(dto.getPhone().trim());
        address.setAddressLine1(dto.getAddressLine1().trim());
        address.setAddressLine2(dto.getAddressLine2() != null ? dto.getAddressLine2().trim() : null);
        address.setCity(dto.getCity().trim());
        address.setDistrict(dto.getDistrict() != null ? dto.getDistrict().trim() : null);
        address.setState(dto.getState().trim());
        address.setPincode(dto.getPincode().trim());
        address.setLandmark(dto.getLandmark() != null ? dto.getLandmark().trim() : null);
        if (dto.getAddressType() != null && !dto.getAddressType().isBlank()) {
            address.setAddressType(dto.getAddressType().trim().toUpperCase());
        }

        Address saved = addressRepository.save(address);
        return toDTO(saved);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found with id: " + addressId));

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIdDesc(userId);
            if (!remaining.isEmpty()) {
                Address nextDefault = remaining.get(0);
                nextDefault.setDefault(true);
                addressRepository.save(nextDefault);
            }
        }
    }

    @Transactional
    public AddressDTO setDefaultAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found with id: " + addressId));

        unsetDefaultAddress(userId);
        address.setDefault(true);
        Address saved = addressRepository.save(address);
        return toDTO(saved);
    }

    private void unsetDefaultAddress(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(addr -> {
            addr.setDefault(false);
            addressRepository.save(addr);
        });
    }

    private void validateAddress(AddressDTO dto) {
        if (dto.getFullName() == null || dto.getFullName().trim().isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (dto.getAddressLine1() == null || dto.getAddressLine1().trim().isBlank()) {
            throw new IllegalArgumentException("Address line 1 is required");
        }
        if (dto.getCity() == null || dto.getCity().trim().isBlank()) {
            throw new IllegalArgumentException("City is required");
        }
        if (dto.getState() == null || dto.getState().trim().isBlank()) {
            throw new IllegalArgumentException("State is required");
        }
        if (dto.getPhone() == null || !PHONE_PATTERN.matcher(dto.getPhone().trim()).matches()) {
            throw new IllegalArgumentException("Phone number must be a valid 10-digit number");
        }
        if (dto.getPincode() == null || !PINCODE_PATTERN.matcher(dto.getPincode().trim()).matches()) {
            throw new IllegalArgumentException("PIN code must be a valid 6-digit Indian postal code");
        }
    }

    private AddressDTO toDTO(Address a) {
        AddressDTO dto = new AddressDTO();
        dto.setId(a.getId());
        dto.setUserId(a.getUser() != null ? a.getUser().getId() : null);
        dto.setFullName(a.getFullName());
        dto.setPhone(a.getPhone());
        dto.setAddressLine1(a.getAddressLine1());
        dto.setAddressLine2(a.getAddressLine2());
        dto.setCity(a.getCity());
        dto.setDistrict(a.getDistrict());
        dto.setState(a.getState());
        dto.setPincode(a.getPincode());
        dto.setLandmark(a.getLandmark());
        dto.setAddressType(a.getAddressType());
        dto.setDefault(a.isDefault());
        return dto;
    }
}
