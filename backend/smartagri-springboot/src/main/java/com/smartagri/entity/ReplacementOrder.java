package com.smartagri.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "replacement_orders", indexes = {
        @Index(name = "idx_ro_return", columnList = "return_request_id"),
        @Index(name = "idx_ro_original", columnList = "original_order_id"),
        @Index(name = "idx_ro_replacement", columnList = "replacement_order_id", unique = true)
})
public class ReplacementOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_order_id", nullable = false)
    private Order originalOrder;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_order_id", nullable = false, unique = true)
    private Order replacementOrder;

    @Column(nullable = false, length = 50)
    private String status = "CREATED";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ReplacementOrder() {}

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "CREATED";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReturnRequest getReturnRequest() {
        return returnRequest;
    }

    public void setReturnRequest(ReturnRequest returnRequest) {
        this.returnRequest = returnRequest;
    }

    public Order getOriginalOrder() {
        return originalOrder;
    }

    public void setOriginalOrder(Order originalOrder) {
        this.originalOrder = originalOrder;
    }

    public Order getReplacementOrder() {
        return replacementOrder;
    }

    public void setReplacementOrder(Order replacementOrder) {
        this.replacementOrder = replacementOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
}
