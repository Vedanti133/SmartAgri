package com.smartagri.dto;

import com.smartagri.entity.PurchaseOrder;
import com.smartagri.entity.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PurchaseOrderDTO {

    private Long id;
    private String poNumber;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private PurchaseOrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String notes;
    private String createdBy;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PurchaseOrderItemDTO> items = new ArrayList<>();
    private Integer totalItemsCount = 0;
    private Integer totalOrderedQuantity = 0;
    private Integer totalReceivedQuantity = 0;
    private Integer totalRemainingQuantity = 0;

    public PurchaseOrderDTO() {}

    public static PurchaseOrderDTO fromEntity(PurchaseOrder po) {
        if (po == null) return null;
        PurchaseOrderDTO dto = new PurchaseOrderDTO();
        dto.setId(po.getId());
        dto.setPoNumber(po.getPoNumber());
        if (po.getSupplier() != null) {
            dto.setSupplierId(po.getSupplier().getId());
            dto.setSupplierCode(po.getSupplier().getSupplierCode());
            dto.setSupplierName(po.getSupplier().getCompanyName());
        }
        dto.setOrderDate(po.getOrderDate());
        dto.setExpectedDeliveryDate(po.getExpectedDeliveryDate());
        dto.setStatus(po.getStatus());
        dto.setSubtotal(po.getSubtotal());
        dto.setTaxAmount(po.getTaxAmount());
        dto.setTotalAmount(po.getTotalAmount());
        dto.setNotes(po.getNotes());
        dto.setCreatedBy(po.getCreatedBy());
        dto.setApprovedBy(po.getApprovedBy());
        dto.setApprovedAt(po.getApprovedAt());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());

        if (po.getItems() != null) {
            dto.setItems(po.getItems().stream()
                    .map(PurchaseOrderItemDTO::fromEntity)
                    .collect(Collectors.toList()));
            dto.setTotalItemsCount(po.getItems().size());
            int ord = 0;
            int rec = 0;
            int rem = 0;
            for (PurchaseOrderItemDTO it : dto.getItems()) {
                ord += (it.getOrderedQuantity() != null ? it.getOrderedQuantity() : 0);
                rec += (it.getReceivedQuantity() != null ? it.getReceivedQuantity() : 0);
                rem += (it.getRemainingQuantity() != null ? it.getRemainingQuantity() : 0);
            }
            dto.setTotalOrderedQuantity(ord);
            dto.setTotalReceivedQuantity(rec);
            dto.setTotalRemainingQuantity(rem);
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseOrderStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<PurchaseOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItemDTO> items) {
        this.items = items;
    }

    public Integer getTotalItemsCount() {
        return totalItemsCount;
    }

    public void setTotalItemsCount(Integer totalItemsCount) {
        this.totalItemsCount = totalItemsCount;
    }

    public Integer getTotalOrderedQuantity() {
        return totalOrderedQuantity;
    }

    public void setTotalOrderedQuantity(Integer totalOrderedQuantity) {
        this.totalOrderedQuantity = totalOrderedQuantity;
    }

    public Integer getTotalReceivedQuantity() {
        return totalReceivedQuantity;
    }

    public void setTotalReceivedQuantity(Integer totalReceivedQuantity) {
        this.totalReceivedQuantity = totalReceivedQuantity;
    }

    public Integer getTotalRemainingQuantity() {
        return totalRemainingQuantity;
    }

    public void setTotalRemainingQuantity(Integer totalRemainingQuantity) {
        this.totalRemainingQuantity = totalRemainingQuantity;
    }
}
