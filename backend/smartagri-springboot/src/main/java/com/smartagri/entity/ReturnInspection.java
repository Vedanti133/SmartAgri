package com.smartagri.entity;

import com.smartagri.enums.InspectionResult;
import com.smartagri.enums.ItemCondition;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_inspections", indexes = {
        @Index(name = "idx_ri_return", columnList = "return_request_id", unique = true)
})
public class ReturnInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false, unique = true)
    private ReturnRequest returnRequest;

    @Column(name = "inspected_by", nullable = false, length = 100)
    private String inspectedBy;

    @Column(name = "inspection_date", nullable = false)
    private LocalDateTime inspectionDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 50)
    private ItemCondition itemCondition = ItemCondition.UNKNOWN;

    @Column(name = "accepted_quantity", nullable = false)
    private Integer acceptedQuantity = 0;

    @Column(name = "rejected_quantity", nullable = false)
    private Integer rejectedQuantity = 0;

    @Column(name = "quarantined_quantity", nullable = false)
    private Integer quarantinedQuantity = 0;

    @Column(name = "inspection_notes", columnDefinition = "TEXT")
    private String inspectionNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InspectionResult result = InspectionResult.ACCEPTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarantine_location_id")
    private WarehouseLocation quarantineLocation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ReturnInspection() {}

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (inspectionDate == null) inspectionDate = LocalDateTime.now();
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

    public String getInspectedBy() {
        return inspectedBy;
    }

    public void setInspectedBy(String inspectedBy) {
        this.inspectedBy = inspectedBy;
    }

    public LocalDateTime getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(LocalDateTime inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public ItemCondition getItemCondition() {
        return itemCondition;
    }

    public void setItemCondition(ItemCondition itemCondition) {
        this.itemCondition = itemCondition;
    }

    public Integer getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public void setAcceptedQuantity(Integer acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
    }

    public Integer getRejectedQuantity() {
        return rejectedQuantity;
    }

    public void setRejectedQuantity(Integer rejectedQuantity) {
        this.rejectedQuantity = rejectedQuantity;
    }

    public Integer getQuarantinedQuantity() {
        return quarantinedQuantity;
    }

    public void setQuarantinedQuantity(Integer quarantinedQuantity) {
        this.quarantinedQuantity = quarantinedQuantity;
    }

    public String getInspectionNotes() {
        return inspectionNotes;
    }

    public void setInspectionNotes(String inspectionNotes) {
        this.inspectionNotes = inspectionNotes;
    }

    public InspectionResult getResult() {
        return result;
    }

    public void setResult(InspectionResult result) {
        this.result = result;
    }

    public WarehouseLocation getQuarantineLocation() {
        return quarantineLocation;
    }

    public void setQuarantineLocation(WarehouseLocation quarantineLocation) {
        this.quarantineLocation = quarantineLocation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
