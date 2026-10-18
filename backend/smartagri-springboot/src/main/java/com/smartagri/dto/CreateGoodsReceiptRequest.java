package com.smartagri.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class CreateGoodsReceiptRequest {

    @NotNull(message = "Purchase order ID is required")
    private Long purchaseOrderId;

    private Long warehouseId;
    private Long locationId;

    private LocalDate receivedDate;
    private String supplierInvoiceNumber;
    private String supplierDeliveryNumber;
    private String packagingCondition;
    private String sealCondition;
    private Boolean labelVerification = true;
    private Boolean quantityVerification = true;
    private String notes;

    @NotEmpty(message = "At least one receipt item is required")
    @Valid
    private List<CreateGoodsReceiptItemRequest> items;

    public CreateGoodsReceiptRequest() {}

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<CreateGoodsReceiptItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreateGoodsReceiptItemRequest> items) {
        this.items = items;
    }
}
