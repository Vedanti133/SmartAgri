package com.smartagri.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public class AssignDeliveryRequest {

    @NotBlank(message = "Delivery partner name is required")
    private String deliveryPartner;

    @NotBlank(message = "Delivery partner phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be a valid 10-digit number")
    private String deliveryPartnerPhone;

    private LocalDate estimatedDeliveryDate;
    private String notes;

    public AssignDeliveryRequest() {}

    public AssignDeliveryRequest(String deliveryPartner, String deliveryPartnerPhone, LocalDate estimatedDeliveryDate, String notes) {
        this.deliveryPartner = deliveryPartner;
        this.deliveryPartnerPhone = deliveryPartnerPhone;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.notes = notes;
    }

    public String getDeliveryPartner() {
        return deliveryPartner;
    }

    public void setDeliveryPartner(String deliveryPartner) {
        this.deliveryPartner = deliveryPartner;
    }

    public String getDeliveryPartnerPhone() {
        return deliveryPartnerPhone;
    }

    public void setDeliveryPartnerPhone(String deliveryPartnerPhone) {
        this.deliveryPartnerPhone = deliveryPartnerPhone;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
