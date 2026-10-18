package com.smartagri.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateDeliveryPartnerRequest {

    @NotBlank(message = "Partner code is required")
    private String partnerCode;

    @NotBlank(message = "Partner name is required")
    private String partnerName;

    private String contactNumber;
    private String trackingUrlTemplate;

    public CreateDeliveryPartnerRequest() {}

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getTrackingUrlTemplate() { return trackingUrlTemplate; }
    public void setTrackingUrlTemplate(String trackingUrlTemplate) { this.trackingUrlTemplate = trackingUrlTemplate; }
}
