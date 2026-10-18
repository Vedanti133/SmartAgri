package com.smartagri.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pick_list_items", indexes = {
        @Index(name = "idx_pli_pl", columnList = "pick_list_id"),
        @Index(name = "idx_pli_batch", columnList = "batch_id")
})
public class PickListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pick_list_id", nullable = false)
    private PickList pickList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fertilizer_id", nullable = false)
    private Fertilizer fertilizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private FertilizerBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @Column(name = "allocation_id")
    private Long allocationId;

    @Column(name = "required_quantity", nullable = false)
    private Integer requiredQuantity;

    @Column(name = "picked_quantity", nullable = false)
    private Integer pickedQuantity = 0;

    @Column(name = "picker", length = 100)
    private String picker;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public PickListItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PickList getPickList() { return pickList; }
    public void setPickList(PickList pickList) { this.pickList = pickList; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }

    public Fertilizer getFertilizer() { return fertilizer; }
    public void setFertilizer(Fertilizer fertilizer) { this.fertilizer = fertilizer; }

    public FertilizerBatch getBatch() { return batch; }
    public void setBatch(FertilizerBatch batch) { this.batch = batch; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public WarehouseLocation getLocation() { return location; }
    public void setLocation(WarehouseLocation location) { this.location = location; }

    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }

    public Integer getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(Integer requiredQuantity) { this.requiredQuantity = requiredQuantity; }

    public Integer getPickedQuantity() { return pickedQuantity; }
    public void setPickedQuantity(Integer pickedQuantity) { this.pickedQuantity = pickedQuantity; }

    public String getPicker() { return picker; }
    public void setPicker(String picker) { this.picker = picker; }

    public LocalDateTime getPickedAt() { return pickedAt; }
    public void setPickedAt(LocalDateTime pickedAt) { this.pickedAt = pickedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
