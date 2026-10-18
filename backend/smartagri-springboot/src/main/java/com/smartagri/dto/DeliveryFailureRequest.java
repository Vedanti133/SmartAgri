package com.smartagri.dto;

import jakarta.validation.constraints.NotBlank;

public class DeliveryFailureRequest {

    @NotBlank(message = "Failure reason is required")
    private String reason;

    private String notes;

    public DeliveryFailureRequest() {}

    public DeliveryFailureRequest(String reason, String notes) {
        this.reason = reason;
        this.notes = notes;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
