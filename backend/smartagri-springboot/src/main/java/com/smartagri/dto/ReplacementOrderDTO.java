package com.smartagri.dto;

import java.time.LocalDateTime;

public class ReplacementOrderDTO {

    private Long id;
    private Long returnRequestId;
    private String returnNumber;
    private Long originalOrderId;
    private String originalOrderTrackingNumber;
    private Long replacementOrderId;
    private String replacementOrderTrackingNumber;
    private String status;
    private String notes;
    private LocalDateTime createdAt;

    public ReplacementOrderDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReturnRequestId() {
        return returnRequestId;
    }

    public void setReturnRequestId(Long returnRequestId) {
        this.returnRequestId = returnRequestId;
    }

    public String getReturnNumber() {
        return returnNumber;
    }

    public void setReturnNumber(String returnNumber) {
        this.returnNumber = returnNumber;
    }

    public Long getOriginalOrderId() {
        return originalOrderId;
    }

    public void setOriginalOrderId(Long originalOrderId) {
        this.originalOrderId = originalOrderId;
    }

    public String getOriginalOrderTrackingNumber() {
        return originalOrderTrackingNumber;
    }

    public void setOriginalOrderTrackingNumber(String originalOrderTrackingNumber) {
        this.originalOrderTrackingNumber = originalOrderTrackingNumber;
    }

    public Long getReplacementOrderId() {
        return replacementOrderId;
    }

    public void setReplacementOrderId(Long replacementOrderId) {
        this.replacementOrderId = replacementOrderId;
    }

    public String getReplacementOrderTrackingNumber() {
        return replacementOrderTrackingNumber;
    }

    public void setReplacementOrderTrackingNumber(String replacementOrderTrackingNumber) {
        this.replacementOrderTrackingNumber = replacementOrderTrackingNumber;
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
