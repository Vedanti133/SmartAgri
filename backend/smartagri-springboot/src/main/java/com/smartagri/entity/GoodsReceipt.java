package com.smartagri.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goods_receipts", indexes = {
        @Index(name = "idx_grn_number", columnList = "grn_number"),
        @Index(name = "idx_grn_po", columnList = "purchase_order_id"),
        @Index(name = "idx_grn_status", columnList = "status")
})
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grn_number", nullable = false, unique = true, length = 50)
    private String grnNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id")
    private WarehouseLocation location;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GoodsReceiptStatus status = GoodsReceiptStatus.DRAFT;

    @Column(name = "supplier_invoice_number", length = 100)
    private String supplierInvoiceNumber;

    @Column(name = "supplier_delivery_number", length = 100)
    private String supplierDeliveryNumber;

    @Column(name = "packaging_condition", length = 100)
    private String packagingCondition;

    @Column(name = "seal_condition", length = 100)
    private String sealCondition;

    @Column(name = "label_verification")
    private Boolean labelVerification = true;

    @Column(name = "quantity_verification")
    private Boolean quantityVerification = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_status", nullable = false, length = 30)
    private QualityInspectionStatus inspectionStatus = QualityInspectionStatus.PENDING;

    @Column(name = "inspection_notes", columnDefinition = "TEXT")
    private String inspectionNotes;

    @Column(name = "inspected_by", length = 100)
    private String inspectedBy;

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GoodsReceiptItem> items = new ArrayList<>();

    public GoodsReceipt() {}

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = GoodsReceiptStatus.DRAFT;
        }
        if (this.inspectionStatus == null) {
            this.inspectionStatus = QualityInspectionStatus.PENDING;
        }
        if (this.receivedDate == null) {
            this.receivedDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(GoodsReceiptItem item) {
        items.add(item);
        item.setGoodsReceipt(this);
    }

    public void removeItem(GoodsReceiptItem item) {
        items.remove(item);
        item.setGoodsReceipt(null);
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

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public WarehouseLocation getLocation() {
        return location;
    }

    public void setLocation(WarehouseLocation location) {
        this.location = location;
    }

    public List<GoodsReceiptItem> getItems() {
        return items;
    }

    public void setItems(List<GoodsReceiptItem> items) {
        this.items = items;
    }
}
