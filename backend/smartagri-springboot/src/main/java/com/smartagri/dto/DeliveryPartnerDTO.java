package com.smartagri.dto;

import java.time.LocalDateTime;

public class DeliveryPartnerDTO {
    private Long id;
    private String partnerCode;
    private String partnerName;
    private String contactNumber;
    private String trackingUrlTemplate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DeliveryPartnerDTO() {}

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
