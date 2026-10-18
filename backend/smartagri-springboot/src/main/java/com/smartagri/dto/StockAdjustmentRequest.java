package com.smartagri.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StockAdjustmentRequest {

    private Long fertilizerId;
    private Long productId;

    @NotBlank(message = "Adjustment type is required (ADD, REMOVE, SET, ADJUSTMENT_IN, ADJUSTMENT_OUT)")
    private String adjustmentType;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotBlank(message = "Audit reason is strictly required for inventory adjustments")
    private String reason;

    private String referenceId;
    private String notes;

    public StockAdjustmentRequest() {
    }

    public Long getFertilizerId() {
        return fertilizerId != null ? fertilizerId : productId;
    }

    public void setFertilizerId(Long fertilizerId) {
        this.fertilizerId = fertilizerId;
    }

    public Long getProductId() {
        return productId != null ? productId : fertilizerId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
