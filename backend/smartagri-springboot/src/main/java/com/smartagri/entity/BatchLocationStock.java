package com.smartagri.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_location_stock",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_batch_location", columnNames = {"batch_id", "location_id"})
        },
        indexes = {
                @Index(name = "idx_bls_batch", columnList = "batch_id"),
                @Index(name = "idx_bls_warehouse", columnList = "warehouse_id"),
                @Index(name = "idx_bls_location", columnList = "location_id")
        }
)
public class BatchLocationStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false)
    private FertilizerBatch batch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BatchLocationStock() {
    }

    public BatchLocationStock(FertilizerBatch batch, Warehouse warehouse, WarehouseLocation location, Integer quantity) {
        this.batch = batch;
        this.warehouse = warehouse;
        this.location = location;
        this.quantity = quantity != null ? quantity : 0;
        this.reservedQuantity = 0;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.quantity == null) {
            this.quantity = 0;
        }
        if (this.reservedQuantity == null) {
            this.reservedQuantity = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public int getAvailableQuantity() {
        int q = quantity != null ? quantity : 0;
        int r = reservedQuantity != null ? reservedQuantity : 0;
        return Math.max(0, q - r);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FertilizerBatch getBatch() {
        return batch;
    }

    public void setBatch(FertilizerBatch batch) {
        this.batch = batch;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
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
