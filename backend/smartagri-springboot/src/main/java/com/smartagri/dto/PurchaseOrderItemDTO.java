package com.smartagri.dto;

import com.smartagri.entity.PurchaseOrderItem;
import java.math.BigDecimal;

public class PurchaseOrderItemDTO {

    private Long id;
    private Long fertilizerId;
    private String fertilizerName;
    private String fertilizerSku;
    private Integer orderedQuantity;
    private Integer receivedQuantity;
    private Integer remainingQuantity;
    private BigDecimal unitCost;
    private BigDecimal taxRate;
    private BigDecimal lineSubtotal;
    private BigDecimal lineTax;
    private BigDecimal lineTotal;
    private String notes;

    public PurchaseOrderItemDTO() {}

    public static PurchaseOrderItemDTO fromEntity(PurchaseOrderItem item) {
        if (item == null) return null;
        PurchaseOrderItemDTO dto = new PurchaseOrderItemDTO();
        dto.setId(item.getId());
        if (item.getFertilizer() != null) {
            dto.setFertilizerId(item.getFertilizer().getId());
            dto.setFertilizerName(item.getFertilizer().getName());
            dto.setFertilizerSku(item.getFertilizer().getSku());
        }
        dto.setOrderedQuantity(item.getOrderedQuantity());
        dto.setReceivedQuantity(item.getReceivedQuantity());
        dto.setRemainingQuantity(item.getRemainingQuantity());
        dto.setUnitCost(item.getUnitCost());
        dto.setTaxRate(item.getTaxRate());
        dto.setLineSubtotal(item.getLineSubtotal());
        dto.setLineTax(item.getLineTax());
        dto.setLineTotal(item.getLineTotal());
        dto.setNotes(item.getNotes());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFertilizerId() {
        return fertilizerId;
    }

    public void setFertilizerId(Long fertilizerId) {
        this.fertilizerId = fertilizerId;
    }

    public String getFertilizerName() {
        return fertilizerName;
    }

    public void setFertilizerName(String fertilizerName) {
        this.fertilizerName = fertilizerName;
    }

    public String getFertilizerSku() {
        return fertilizerSku;
    }

    public void setFertilizerSku(String fertilizerSku) {
        this.fertilizerSku = fertilizerSku;
    }

    public Integer getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(Integer orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public Integer getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(Integer remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getLineSubtotal() {
        return lineSubtotal;
    }

    public void setLineSubtotal(BigDecimal lineSubtotal) {
        this.lineSubtotal = lineSubtotal;
    }

    public BigDecimal getLineTax() {
        return lineTax;
    }

    public void setLineTax(BigDecimal lineTax) {
        this.lineTax = lineTax;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
