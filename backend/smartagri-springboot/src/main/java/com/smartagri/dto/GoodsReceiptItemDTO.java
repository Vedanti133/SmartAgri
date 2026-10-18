package com.smartagri.dto;

import com.smartagri.entity.GoodsReceiptItem;
import java.math.BigDecimal;
import java.time.LocalDate;

public class GoodsReceiptItemDTO {

    private Long id;
    private Long purchaseOrderItemId;
    private Long fertilizerId;
    private String fertilizerName;
    private String fertilizerSku;
    private Integer receivedQuantity;
    private Integer acceptedQuantity;
    private Integer rejectedQuantity;
    private String rejectionReason;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private String supplierReference;
    private BigDecimal unitCost;
    private String notes;

    public GoodsReceiptItemDTO() {}

    public static GoodsReceiptItemDTO fromEntity(GoodsReceiptItem item) {
        if (item == null) return null;
        GoodsReceiptItemDTO dto = new GoodsReceiptItemDTO();
        dto.setId(item.getId());
        if (item.getPurchaseOrderItem() != null) {
            dto.setPurchaseOrderItemId(item.getPurchaseOrderItem().getId());
        }
        if (item.getFertilizer() != null) {
            dto.setFertilizerId(item.getFertilizer().getId());
            dto.setFertilizerName(item.getFertilizer().getName());
            dto.setFertilizerSku(item.getFertilizer().getSku());
        }
        dto.setReceivedQuantity(item.getReceivedQuantity());
        dto.setAcceptedQuantity(item.getAcceptedQuantity());
        dto.setRejectedQuantity(item.getRejectedQuantity());
        dto.setRejectionReason(item.getRejectionReason());
        dto.setBatchNumber(item.getBatchNumber());
        dto.setManufacturingDate(item.getManufacturingDate());
        dto.setExpiryDate(item.getExpiryDate());
        dto.setSupplierReference(item.getSupplierReference());
        dto.setUnitCost(item.getUnitCost());
        dto.setNotes(item.getNotes());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPurchaseOrderItemId() {
        return purchaseOrderItemId;
    }

    public void setPurchaseOrderItemId(Long purchaseOrderItemId) {
        this.purchaseOrderItemId = purchaseOrderItemId;
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

    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public Integer getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public void setAcceptedQuantity(Integer acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
    }

    public Integer getRejectedQuantity() {
        return rejectedQuantity;
    }

    public void setRejectedQuantity(Integer rejectedQuantity) {
        this.rejectedQuantity = rejectedQuantity;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public void setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getSupplierReference() {
        return supplierReference;
    }

    public void setSupplierReference(String supplierReference) {
        this.supplierReference = supplierReference;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
