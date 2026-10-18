package com.smartagri.dto;

import com.smartagri.enums.ReturnReason;
import com.smartagri.enums.ReturnStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReturnRequestDTO {

    private Long id;
    private String returnNumber;
    private Long orderId;
    private String orderTrackingNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private ReturnStatus status;
    private ReturnReason reason;
    private String customerComments;
    private String adminNotes;
    private String rejectionReason;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReturnRequestItemDTO> items = new ArrayList<>();
    private ReturnShipmentDTO shipment;
    private ReturnInspectionDTO inspection;
    private RefundDTO refund;
    private ReplacementOrderDTO replacement;

    public ReturnRequestDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReturnNumber() {
        return returnNumber;
    }

    public void setReturnNumber(String returnNumber) {
        this.returnNumber = returnNumber;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderTrackingNumber() {
        return orderTrackingNumber;
    }

    public void setOrderTrackingNumber(String orderTrackingNumber) {
        this.orderTrackingNumber = orderTrackingNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public ReturnStatus getStatus() {
        return status;
    }

    public void setStatus(ReturnStatus status) {
        this.status = status;
    }

    public ReturnReason getReason() {
        return reason;
    }

    public void setReason(ReturnReason reason) {
        this.reason = reason;
    }

    public String getCustomerComments() {
        return customerComments;
    }

    public void setCustomerComments(String customerComments) {
        this.customerComments = customerComments;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
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

    public List<ReturnRequestItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ReturnRequestItemDTO> items) {
        this.items = items;
    }

    public ReturnShipmentDTO getShipment() {
        return shipment;
    }

    public void setShipment(ReturnShipmentDTO shipment) {
        this.shipment = shipment;
    }

    public ReturnInspectionDTO getInspection() {
        return inspection;
    }

    public void setInspection(ReturnInspectionDTO inspection) {
        this.inspection = inspection;
    }

    public RefundDTO getRefund() {
        return refund;
    }

    public void setRefund(RefundDTO refund) {
        this.refund = refund;
    }

    public ReplacementOrderDTO getReplacement() {
        return replacement;
    }

    public void setReplacement(ReplacementOrderDTO replacement) {
        this.replacement = replacement;
    }
}
