package com.smartagri.service;

import com.smartagri.dto.CreateDeliveryPartnerRequest;
import com.smartagri.dto.DeliveryPartnerDTO;
import com.smartagri.entity.DeliveryPartner;
import com.smartagri.enums.DeliveryPartnerStatus;
import com.smartagri.repository.DeliveryPartnerRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class DeliveryPartnerService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryPartnerService.class);

    private final DeliveryPartnerRepository deliveryPartnerRepository;

    public DeliveryPartnerService(DeliveryPartnerRepository deliveryPartnerRepository) {
        this.deliveryPartnerRepository = deliveryPartnerRepository;
    }

    @PostConstruct
    @Transactional
    public void seedDefaultDeliveryPartners() {
        seedPartnerIfMissing("KISAN_SPEED", "Kisan Speed Logistics", "9822998877", "https://track.kisanspeed.com/?tracking=%s");
        seedPartnerIfMissing("DELHIVERY", "Delhivery Express", "18001036354", "https://www.delhivery.com/track/package/%s");
        seedPartnerIfMissing("BLUEDART", "Blue Dart Express", "18602331234", "https://www.bluedart.com/tracking/%s");
        seedPartnerIfMissing("DTDC", "DTDC Express", "08033004444", "https://www.dtdc.in/tracking/%s");
        seedPartnerIfMissing("SPEED_POST", "India Post Speed Post", "18002666868", "https://www.indiapost.gov.in/track/%s");
    }

    private void seedPartnerIfMissing(String code, String name, String phone, String template) {
        if (deliveryPartnerRepository.findByPartnerCode(code).isEmpty()) {
            DeliveryPartner partner = new DeliveryPartner(code, name, phone, template);
            deliveryPartnerRepository.save(partner);
            log.info("Seeded delivery partner: {} ({})", name, code);
        }
    }

    @Transactional(readOnly = true)
    public List<DeliveryPartnerDTO> getAllPartners(String statusFilter) {
        List<DeliveryPartner> list;
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter.trim())) {
            try {
                DeliveryPartnerStatus st = DeliveryPartnerStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT));
                list = deliveryPartnerRepository.findByStatus(st);
            } catch (IllegalArgumentException e) {
                list = deliveryPartnerRepository.findAll();
            }
        } else {
            list = deliveryPartnerRepository.findAll();
        }
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeliveryPartnerDTO getPartnerById(Long id) {
        DeliveryPartner partner = deliveryPartnerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Delivery partner not found with id: " + id));
        return toDTO(partner);
    }

    @Transactional
    public DeliveryPartnerDTO createPartner(CreateDeliveryPartnerRequest request) {
        String code = request.getPartnerCode().trim().toUpperCase(Locale.ROOT);
        if (deliveryPartnerRepository.findByPartnerCode(code).isPresent()) {
            throw new IllegalStateException("Delivery partner code '" + code + "' already exists");
        }

        DeliveryPartner partner = new DeliveryPartner(
                code,
                request.getPartnerName().trim(),
                request.getContactNumber() != null ? request.getContactNumber().trim() : null,
                request.getTrackingUrlTemplate() != null ? request.getTrackingUrlTemplate().trim() : null
        );

        DeliveryPartner saved = deliveryPartnerRepository.save(partner);
        log.info("Created delivery partner: {} ({})", saved.getPartnerName(), saved.getPartnerCode());
        return toDTO(saved);
    }

    @Transactional
    public DeliveryPartnerDTO updatePartner(Long id, CreateDeliveryPartnerRequest request) {
        DeliveryPartner partner = deliveryPartnerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Delivery partner not found with id: " + id));

        if (request.getPartnerName() != null && !request.getPartnerName().isBlank()) {
            partner.setPartnerName(request.getPartnerName().trim());
        }
        if (request.getContactNumber() != null) {
            partner.setContactNumber(request.getContactNumber().trim());
        }
        if (request.getTrackingUrlTemplate() != null) {
            partner.setTrackingUrlTemplate(request.getTrackingUrlTemplate().trim());
        }

        DeliveryPartner saved = deliveryPartnerRepository.save(partner);
        return toDTO(saved);
    }

    @Transactional
    public DeliveryPartnerDTO updateStatus(Long id, String statusStr) {
        DeliveryPartner partner = deliveryPartnerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Delivery partner not found with id: " + id));

        DeliveryPartnerStatus st;
        try {
            st = DeliveryPartnerStatus.valueOf(statusStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr);
        }

        partner.setStatus(st);
        DeliveryPartner saved = deliveryPartnerRepository.save(partner);
        log.info("Updated delivery partner #{} status to {}", saved.getId(), st);
        return toDTO(saved);
    }

    public DeliveryPartnerDTO toDTO(DeliveryPartner p) {
        DeliveryPartnerDTO dto = new DeliveryPartnerDTO();
        dto.setId(p.getId());
        dto.setPartnerCode(p.getPartnerCode());
        dto.setPartnerName(p.getPartnerName());
        dto.setContactNumber(p.getContactNumber());
        dto.setTrackingUrlTemplate(p.getTrackingUrlTemplate());
        dto.setStatus(p.getStatus() != null ? p.getStatus().name() : "ACTIVE");
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}
