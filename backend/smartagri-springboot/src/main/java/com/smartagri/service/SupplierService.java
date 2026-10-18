package com.smartagri.service;

import com.smartagri.dto.CreateSupplierRequest;
import com.smartagri.dto.PurchaseOrderDTO;
import com.smartagri.dto.SupplierDTO;
import com.smartagri.dto.UpdateSupplierRequest;
import com.smartagri.entity.Supplier;
import com.smartagri.entity.SupplierStatus;
import com.smartagri.repository.PurchaseOrderRepository;
import com.smartagri.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierService.class);

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public SupplierService(SupplierRepository supplierRepository,
                           PurchaseOrderRepository purchaseOrderRepository) {
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<SupplierDTO> getAllSuppliers(SupplierStatus status, String search) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        List<Supplier> list = supplierRepository.searchSuppliers(status, cleanSearch);
        return list.stream().map(SupplierDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierDTO getSupplierById(Long id) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Supplier not found with id: " + id));
        return SupplierDTO.fromEntity(s);
    }

    @Transactional(readOnly = true)
    public SupplierDTO getSupplierByCode(String code) {
        Supplier s = supplierRepository.findBySupplierCode(code)
                .orElseThrow(() -> new NoSuchElementException("Supplier not found with code: " + code));
        return SupplierDTO.fromEntity(s);
    }

    @Transactional
    public SupplierDTO createSupplier(CreateSupplierRequest req) {
        if (req.getSupplierCode() == null || req.getSupplierCode().isBlank()) {
            throw new IllegalArgumentException("Supplier code is required");
        }
        if (req.getCompanyName() == null || req.getCompanyName().isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }

        String cleanCode = req.getSupplierCode().trim().toUpperCase();
        if (supplierRepository.existsBySupplierCode(cleanCode)) {
            throw new IllegalStateException("Supplier code '" + cleanCode + "' already exists");
        }

        Supplier s = new Supplier();
        s.setSupplierCode(cleanCode);
        s.setCompanyName(req.getCompanyName().trim());
        s.setContactPerson(req.getContactPerson() != null ? req.getContactPerson().trim() : null);
        s.setEmail(req.getEmail() != null ? req.getEmail().trim() : null);
        s.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);
        s.setAlternatePhone(req.getAlternatePhone() != null ? req.getAlternatePhone().trim() : null);
        s.setAddress(req.getAddress() != null ? req.getAddress().trim() : null);
        s.setCity(req.getCity() != null ? req.getCity().trim() : null);
        s.setState(req.getState() != null ? req.getState().trim() : null);
        s.setPostalCode(req.getPostalCode() != null ? req.getPostalCode().trim() : null);
        s.setGstNumber(req.getGstNumber() != null ? req.getGstNumber().trim().toUpperCase() : null);
        s.setPanNumber(req.getPanNumber() != null ? req.getPanNumber().trim().toUpperCase() : null);
        s.setPaymentTerms(req.getPaymentTerms() != null ? req.getPaymentTerms().trim() : null);
        s.setStatus(SupplierStatus.ACTIVE);
        s.setNotes(req.getNotes() != null ? req.getNotes().trim() : null);

        Supplier saved = supplierRepository.save(s);
        log.info("Supplier created: #{} [{}]", saved.getId(), saved.getSupplierCode());
        return SupplierDTO.fromEntity(saved);
    }

    @Transactional
    public SupplierDTO updateSupplier(Long id, UpdateSupplierRequest req) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Supplier not found with id: " + id));

        if (req.getCompanyName() != null && !req.getCompanyName().isBlank()) {
            s.setCompanyName(req.getCompanyName().trim());
        }
        if (req.getContactPerson() != null) s.setContactPerson(req.getContactPerson().trim());
        if (req.getEmail() != null) s.setEmail(req.getEmail().trim());
        if (req.getPhone() != null) s.setPhone(req.getPhone().trim());
        if (req.getAlternatePhone() != null) s.setAlternatePhone(req.getAlternatePhone().trim());
        if (req.getAddress() != null) s.setAddress(req.getAddress().trim());
        if (req.getCity() != null) s.setCity(req.getCity().trim());
        if (req.getState() != null) s.setState(req.getState().trim());
        if (req.getPostalCode() != null) s.setPostalCode(req.getPostalCode().trim());
        if (req.getGstNumber() != null) s.setGstNumber(req.getGstNumber().trim().toUpperCase());
        if (req.getPanNumber() != null) s.setPanNumber(req.getPanNumber().trim().toUpperCase());
        if (req.getPaymentTerms() != null) s.setPaymentTerms(req.getPaymentTerms().trim());
        if (req.getStatus() != null) s.setStatus(req.getStatus());
        if (req.getNotes() != null) s.setNotes(req.getNotes().trim());

        Supplier saved = supplierRepository.save(s);
        log.info("Supplier updated: #{}", saved.getId());
        return SupplierDTO.fromEntity(saved);
    }

    @Transactional
    public SupplierDTO activateSupplier(Long id) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Supplier not found with id: " + id));
        s.setStatus(SupplierStatus.ACTIVE);
        return SupplierDTO.fromEntity(supplierRepository.save(s));
    }

    @Transactional
    public SupplierDTO deactivateSupplier(Long id) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Supplier not found with id: " + id));
        s.setStatus(SupplierStatus.INACTIVE);
        return SupplierDTO.fromEntity(supplierRepository.save(s));
    }

    @Transactional
    public SupplierDTO blockSupplier(Long id) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Supplier not found with id: " + id));
        s.setStatus(SupplierStatus.BLOCKED);
        return SupplierDTO.fromEntity(supplierRepository.save(s));
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderDTO> getSupplierPurchaseOrders(Long supplierId) {
        if (!supplierRepository.existsById(supplierId)) {
            throw new NoSuchElementException("Supplier not found with id: " + supplierId);
        }
        return purchaseOrderRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId)
                .stream()
                .map(PurchaseOrderDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
