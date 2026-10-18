package com.smartagri.dto;

import com.smartagri.entity.GoodsReceipt;
import com.smartagri.entity.GoodsReceiptStatus;
import com.smartagri.entity.QualityInspectionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GoodsReceiptDTO {

    private Long id;
    private String grnNumber;
    private Long purchaseOrderId;
    private String poNumber;
    private Long supplierId;
    private String supplierName;
    private LocalDate receivedDate;
    private String receivedBy;
    private GoodsReceiptStatus status;
    private String supplierInvoiceNumber;
    private String supplierDeliveryNumber;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long locationId;
    private String locationCode;

    private String packagingCondition;
    private String sealCondition;
    private Boolean labelVerification;
    private Boolean quantityVerification;
    private QualityInspectionStatus inspectionStatus;
    private String inspectionNotes;
    private String inspectedBy;
    private LocalDateTime inspectedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<GoodsReceiptItemDTO> items = new ArrayList<>();
    private Integer totalReceivedQuantity = 0;
    private Integer totalAcceptedQuantity = 0;
    private Integer totalRejectedQuantity = 0;

    public GoodsReceiptDTO() {}

    public static GoodsReceiptDTO fromEntity(GoodsReceipt grn) {
        if (grn == null) return null;
        GoodsReceiptDTO dto = new GoodsReceiptDTO();
        dto.setId(grn.getId());
        dto.setGrnNumber(grn.getGrnNumber());
        if (grn.getPurchaseOrder() != null) {
            dto.setPurchaseOrderId(grn.getPurchaseOrder().getId());
            dto.setPoNumber(grn.getPurchaseOrder().getPoNumber());
            if (grn.getPurchaseOrder().getSupplier() != null) {
                dto.setSupplierId(grn.getPurchaseOrder().getSupplier().getId());
                dto.setSupplierName(grn.getPurchaseOrder().getSupplier().getCompanyName());
            }
        }
        if (grn.getWarehouse() != null) {
            dto.setWarehouseId(grn.getWarehouse().getId());
            dto.setWarehouseCode(grn.getWarehouse().getWarehouseCode());
            dto.setWarehouseName(grn.getWarehouse().getWarehouseName());
        }
        if (grn.getLocation() != null) {
            dto.setLocationId(grn.getLocation().getId());
            dto.setLocationCode(grn.getLocation().getLocationCode());
        }
        dto.setReceivedDate(grn.getReceivedDate());
        dto.setReceivedBy(grn.getReceivedBy());
        dto.setStatus(grn.getStatus());
        dto.setSupplierInvoiceNumber(grn.getSupplierInvoiceNumber());
        dto.setSupplierDeliveryNumber(grn.getSupplierDeliveryNumber());
        dto.setPackagingCondition(grn.getPackagingCondition());
        dto.setSealCondition(grn.getSealCondition());
        dto.setLabelVerification(grn.getLabelVerification());
        dto.setQuantityVerification(grn.getQuantityVerification());
        dto.setInspectionStatus(grn.getInspectionStatus());
        dto.setInspectionNotes(grn.getInspectionNotes());
        dto.setInspectedBy(grn.getInspectedBy());
        dto.setInspectedAt(grn.getInspectedAt());
        dto.setNotes(grn.getNotes());
        dto.setCreatedAt(grn.getCreatedAt());
        dto.setUpdatedAt(grn.getUpdatedAt());

        if (grn.getItems() != null) {
            dto.setItems(grn.getItems().stream()
                    .map(GoodsReceiptItemDTO::fromEntity)
                    .collect(Collectors.toList()));
            int rec = 0;
            int acc = 0;
            int rej = 0;
            for (GoodsReceiptItemDTO it : dto.getItems()) {
                rec += (it.getReceivedQuantity() != null ? it.getReceivedQuantity() : 0);
                acc += (it.getAcceptedQuantity() != null ? it.getAcceptedQuantity() : 0);
                rej += (it.getRejectedQuantity() != null ? it.getRejectedQuantity() : 0);
            }
            dto.setTotalReceivedQuantity(rec);
            dto.setTotalAcceptedQuantity(acc);
            dto.setTotalRejectedQuantity(rej);
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGrnNumber() {
        return grnNumber;
    }

    public void setGrnNumber(String grnNumber) {
        this.grnNumber = grnNumber;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
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

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public GoodsReceiptStatus getStatus() {
        return status;
    }

    public void setStatus(GoodsReceiptStatus status) {
        this.status = status;
    }

    public String getSupplierInvoiceNumber() {
        return supplierInvoiceNumber;
    }

    public void setSupplierInvoiceNumber(String supplierInvoiceNumber) {
        this.supplierInvoiceNumber = supplierInvoiceNumber;
    }

    public String getSupplierDeliveryNumber() {
        return supplierDeliveryNumber;
    }

    public void setSupplierDeliveryNumber(String supplierDeliveryNumber) {
        this.supplierDeliveryNumber = supplierDeliveryNumber;
    }

    public String getPackagingCondition() {
        return packagingCondition;
    }

    public void setPackagingCondition(String packagingCondition) {
        this.packagingCondition = packagingCondition;
    }

    public String getSealCondition() {
        return sealCondition;
    }

    public void setSealCondition(String sealCondition) {
        this.sealCondition = sealCondition;
    }

    public Boolean getLabelVerification() {
        return labelVerification;
    }

    public void setLabelVerification(Boolean labelVerification) {
        this.labelVerification = labelVerification;
    }

    public Boolean getQuantityVerification() {
        return quantityVerification;
    }

    public void setQuantityVerification(Boolean quantityVerification) {
        this.quantityVerification = quantityVerification;
    }

    public QualityInspectionStatus getInspectionStatus() {
        return inspectionStatus;
    }

    public void setInspectionStatus(QualityInspectionStatus inspectionStatus) {
        this.inspectionStatus = inspectionStatus;
    }

    public String getInspectionNotes() {
        return inspectionNotes;
    }

    public void setInspectionNotes(String inspectionNotes) {
        this.inspectionNotes = inspectionNotes;
    }

    public String getInspectedBy() {
        return inspectedBy;
    }

    public void setInspectedBy(String inspectedBy) {
        this.inspectedBy = inspectedBy;
    }

    public LocalDateTime getInspectedAt() {
        return inspectedAt;
    }

    public void setInspectedAt(LocalDateTime inspectedAt) {
        this.inspectedAt = inspectedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public List<GoodsReceiptItemDTO> getItems() {
        return items;
    }

    public void setItems(List<GoodsReceiptItemDTO> items) {
        this.items = items;
    }

    public Integer getTotalReceivedQuantity() {
        return totalReceivedQuantity;
    }

    public void setTotalReceivedQuantity(Integer totalReceivedQuantity) {
        this.totalReceivedQuantity = totalReceivedQuantity;
    }

    public Integer getTotalAcceptedQuantity() {
        return totalAcceptedQuantity;
    }

    public void setTotalAcceptedQuantity(Integer totalAcceptedQuantity) {
        this.totalAcceptedQuantity = totalAcceptedQuantity;
    }

    public Integer getTotalRejectedQuantity() {
        return totalRejectedQuantity;
    }

    public void setTotalRejectedQuantity(Integer totalRejectedQuantity) {
        this.totalRejectedQuantity = totalRejectedQuantity;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }
}
