package com.smartagri.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class StockInRequest {

    private Long fertilizerId;
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String referenceId;
    private String reason;
    private String supplier;
    private BigDecimal unitCost;
    private BigDecimal purchasePrice;
    private Integer lowStockThreshold;
    private String notes;

    public StockInRequest() {
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getUnitCost() {
        return unitCost != null ? unitCost : purchasePrice;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice != null ? purchasePrice : unitCost;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
