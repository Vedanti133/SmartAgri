package com.smartagri.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transfer_items",
        indexes = {
                @Index(name = "idx_sti_transfer", columnList = "stock_transfer_id"),
                @Index(name = "idx_sti_fertilizer", columnList = "fertilizer_id"),
                @Index(name = "idx_sti_batch", columnList = "batch_id"),
                @Index(name = "idx_sti_source_loc", columnList = "source_location_id"),
                @Index(name = "idx_sti_dest_loc", columnList = "destination_location_id")
        }
)
public class StockTransferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_transfer_id", nullable = false)
    private StockTransfer stockTransfer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fertilizer_id", nullable = false)
    private Fertilizer fertilizer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false)
    private FertilizerBatch batch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_location_id", nullable = false)
    private WarehouseLocation sourceLocation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_location_id", nullable = false)
    private WarehouseLocation destinationLocation;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "dispatched_quantity", nullable = false)
    private Integer dispatchedQuantity = 0;

    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public StockTransferItem() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.dispatchedQuantity == null) {
            this.dispatchedQuantity = 0;
        }
        if (this.receivedQuantity == null) {
            this.receivedQuantity = 0;
        }
    }

    public int getRemainingToReceive() {
        int d = dispatchedQuantity != null ? dispatchedQuantity : 0;
        int r = receivedQuantity != null ? receivedQuantity : 0;
        return Math.max(0, d - r);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StockTransfer getStockTransfer() {
        return stockTransfer;
    }

    public void setStockTransfer(StockTransfer stockTransfer) {
        this.stockTransfer = stockTransfer;
    }

    public Fertilizer getFertilizer() {
        return fertilizer;
    }

    public void setFertilizer(Fertilizer fertilizer) {
        this.fertilizer = fertilizer;
    }

    public FertilizerBatch getBatch() {
        return batch;
    }

    public void setBatch(FertilizerBatch batch) {
        this.batch = batch;
    }

    public WarehouseLocation getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(WarehouseLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public WarehouseLocation getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(WarehouseLocation destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Integer getDispatchedQuantity() {
        return dispatchedQuantity;
    }

    public void setDispatchedQuantity(Integer dispatchedQuantity) {
        this.dispatchedQuantity = dispatchedQuantity;
    }

    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
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
}
