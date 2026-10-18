package com.smartagri.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BatchDamageRequest {

    @NotNull(message = "Damaged quantity is required")
    @Min(value = 1, message = "Damaged quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Reason for damage report is required")
    private String reason;

    private String notes;

    public BatchDamageRequest() {
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
