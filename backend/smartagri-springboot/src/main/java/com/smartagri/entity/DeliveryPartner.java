package com.smartagri.entity;

import com.smartagri.enums.DeliveryPartnerStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_partners")
public class DeliveryPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_code", nullable = false, unique = true, length = 50)
    private String partnerCode;

    @Column(name = "partner_name", nullable = false, length = 100)
    private String partnerName;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Column(name = "tracking_url_template", length = 255)
    private String trackingUrlTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryPartnerStatus status = DeliveryPartnerStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public DeliveryPartner() {}

    public DeliveryPartner(String partnerCode, String partnerName, String contactNumber, String trackingUrlTemplate) {
        this.partnerCode = partnerCode;
        this.partnerName = partnerName;
        this.contactNumber = contactNumber;
        this.trackingUrlTemplate = trackingUrlTemplate;
        this.status = DeliveryPartnerStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getTrackingUrlTemplate() { return trackingUrlTemplate; }
    public void setTrackingUrlTemplate(String trackingUrlTemplate) { this.trackingUrlTemplate = trackingUrlTemplate; }

    public DeliveryPartnerStatus getStatus() { return status; }
    public void setStatus(DeliveryPartnerStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
