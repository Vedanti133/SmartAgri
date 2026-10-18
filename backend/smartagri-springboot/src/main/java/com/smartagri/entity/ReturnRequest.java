package com.smartagri.entity;

import com.smartagri.enums.ReturnReason;
import com.smartagri.enums.ReturnStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_requests", indexes = {
        @Index(name = "idx_ret_number", columnList = "return_number", unique = true),
        @Index(name = "idx_ret_order", columnList = "order_id"),
        @Index(name = "idx_ret_customer", columnList = "customer_id"),
        @Index(name = "idx_ret_status", columnList = "status"),
        @Index(name = "idx_ret_created", columnList = "created_at")
})
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_number", nullable = false, unique = true, length = 50)
    private String returnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReturnReason reason;

    @Column(name = "customer_comments", columnDefinition = "TEXT")
    private String customerComments;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnRequestItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private ReturnShipment shipment;

    @OneToOne(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private ReturnInspection inspection;

    @OneToOne(mappedBy = "returnRequest", cascade = CascadeType.ALL)
    private Refund refund;

    public ReturnRequest() {}

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (requestedAt == null) requestedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(ReturnRequestItem item) {
        items.add(item);
        item.setReturnRequest(this);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReturnNumber() { return returnNumber; }
    public void setReturnNumber(String returnNumber) { this.returnNumber = returnNumber; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public ReturnStatus getStatus() { return status; }
    public void setStatus(ReturnStatus status) { this.status = status; }

    public ReturnReason getReason() { return reason; }
    public void setReason(ReturnReason reason) { this.reason = reason; }

    public String getCustomerComments() { return customerComments; }
    public void setCustomerComments(String customerComments) { this.customerComments = customerComments; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<ReturnRequestItem> getItems() { return items; }
    public void setItems(List<ReturnRequestItem> items) { this.items = items; }

    public ReturnShipment getShipment() { return shipment; }
    public void setShipment(ReturnShipment shipment) { this.shipment = shipment; }

    public ReturnInspection getInspection() { return inspection; }
    public void setInspection(ReturnInspection inspection) { this.inspection = inspection; }

    public Refund getRefund() { return refund; }
    public void setRefund(Refund refund) { this.refund = refund; }
}
