package com.smartagri.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fertilizer_batches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fertilizer_batch", columnNames = {"fertilizer_id", "batch_number"})
        },
        indexes = {
                @Index(name = "idx_batch_fertilizer", columnList = "fertilizer_id"),
                @Index(name = "idx_batch_expiry", columnList = "expiry_date"),
                @Index(name = "idx_batch_status", columnList = "status"),
                @Index(name = "idx_batch_number", columnList = "batch_number")
        }
)
public class FertilizerBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fertilizer_id", nullable = false)
    private Fertilizer fertilizer;

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity;

    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "supplier_name", length = 255)
    private String supplierName;

    @Column(name = "supplier_reference", length = 100)
    private String supplierReference;

    @Column(name = "purchase_order_number", length = 100)
    private String purchaseOrderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchStatus status = BatchStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public FertilizerBatch() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.currentQuantity == null) {
            this.currentQuantity = 0;
        }
        if (this.reservedQuantity == null) {
            this.reservedQuantity = 0;
        }
        if (this.status == null) {
            this.status = BatchStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public int getAvailableQuantity() {
        int curr = currentQuantity != null ? currentQuantity : 0;
        int res = reservedQuantity != null ? reservedQuantity : 0;
        return Math.max(0, curr - res);
    }

    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }

    public boolean isNearExpiry() {
        return isNearExpiry(30);
    }

    public boolean isNearExpiry(int warningDays) {
        if (isExpired() || expiryDate == null) {
            return false;
        }
        LocalDate threshold = LocalDate.now().plusDays(warningDays);
        return !expiryDate.isAfter(threshold);
    }

    public boolean isAvailableForSale() {
        return status != BatchStatus.BLOCKED && !isExpired() && getAvailableQuantity() > 0;
    }

    public BatchStatus computeDynamicStatus() {
        if (this.status == BatchStatus.BLOCKED) {
            return BatchStatus.BLOCKED;
        }
        if (isExpired()) {
            return BatchStatus.EXPIRED;
        }
        if (getAvailableQuantity() <= 0) {
            return BatchStatus.DEPLETED;
        }
        if (isNearExpiry(30)) {
            return BatchStatus.NEAR_EXPIRY;
        }
        return BatchStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Fertilizer getFertilizer() {
        return fertilizer;
    }

    public void setFertilizer(Fertilizer fertilizer) {
        this.fertilizer = fertilizer;
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

    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(Integer initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public Integer getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(Integer currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierReference() {
        return supplierReference;
    }

    public void setSupplierReference(String supplierReference) {
        this.supplierReference = supplierReference;
    }

    public String getPurchaseOrderNumber() {
        return purchaseOrderNumber;
    }

    public void setPurchaseOrderNumber(String purchaseOrderNumber) {
        this.purchaseOrderNumber = purchaseOrderNumber;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
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
}
