package com.smartagri.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BatchAdjustmentRequest {

    @NotBlank(message = "Adjustment type is required (ADD, REMOVE, SET)")
    private String adjustmentType;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    @NotBlank(message = "Audit reason is strictly required for batch adjustment")
    private String reason;

    private String notes;

    public BatchAdjustmentRequest() {
    }

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
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
