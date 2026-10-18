package com.smartagri.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_item_batches",
        indexes = {
                @Index(name = "idx_oib_order_item", columnList = "order_item_id"),
                @Index(name = "idx_oib_batch", columnList = "batch_id")
        }
)
public class OrderItemBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false)
    private FertilizerBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private WarehouseLocation location;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public OrderItemBatch() {
    }

    public OrderItemBatch(OrderItem orderItem, FertilizerBatch batch, Integer quantity) {
        this.orderItem = orderItem;
        this.batch = batch;
        this.quantity = quantity;
    }

    public OrderItemBatch(OrderItem orderItem, FertilizerBatch batch, Integer quantity, Warehouse warehouse, WarehouseLocation location) {
        this.orderItem = orderItem;
        this.batch = batch;
        this.quantity = quantity;
        this.warehouse = warehouse;
        this.location = location;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
