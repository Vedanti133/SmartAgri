package com.smartagri.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fulfillment_allocations", indexes = {
        @Index(name = "idx_fa_order", columnList = "order_id"),
        @Index(name = "idx_fa_item", columnList = "order_item_id"),
        @Index(name = "idx_fa_wh", columnList = "warehouse_id"),
        @Index(name = "idx_fa_batch", columnList = "batch_id"),
        @Index(name = "idx_fa_status", columnList = "status")
})
public class FulfillmentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allocation_number", nullable = false, unique = true, length = 50)
    private String allocationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private FertilizerBatch batch;

    @Column(name = "allocated_quantity", nullable = false)
    private Integer allocatedQuantity;

    @Column(name = "picked_quantity", nullable = false)
    private Integer pickedQuantity = 0;

    @Column(name = "packed_quantity", nullable = false)
    private Integer packedQuantity = 0;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ALLOCATED";

    @Column(name = "allocated_at", nullable = false)
    private LocalDateTime allocatedAt;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "packed_at")
    private LocalDateTime packedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public FulfillmentAllocation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAllocationNumber() { return allocationNumber; }
    public void setAllocationNumber(String allocationNumber) { this.allocationNumber = allocationNumber; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public WarehouseLocation getLocation() { return location; }
    public void setLocation(WarehouseLocation location) { this.location = location; }

    public FertilizerBatch getBatch() { return batch; }
    public void setBatch(FertilizerBatch batch) { this.batch = batch; }

    public Integer getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(Integer allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }

    public Integer getPickedQuantity() { return pickedQuantity; }
    public void setPickedQuantity(Integer pickedQuantity) { this.pickedQuantity = pickedQuantity; }

    public Integer getPackedQuantity() { return packedQuantity; }
    public void setPackedQuantity(Integer packedQuantity) { this.packedQuantity = packedQuantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(LocalDateTime allocatedAt) { this.allocatedAt = allocatedAt; }

    public LocalDateTime getPickedAt() { return pickedAt; }
    public void setPickedAt(LocalDateTime pickedAt) { this.pickedAt = pickedAt; }

    public LocalDateTime getPackedAt() { return packedAt; }
    public void setPackedAt(LocalDateTime packedAt) { this.packedAt = packedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
