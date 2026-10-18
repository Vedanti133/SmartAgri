package com.smartagri.dto;

import java.math.BigDecimal;

public class ProcurementDashboardDTO {

    private long totalSuppliers;
    private long activeSuppliers;
    private long draftPOs;
    private long pendingApprovalPOs;
    private long openPurchaseOrders;
    private long partiallyReceivedPOs;
    private long fullyReceivedPOs;
    private long pendingGRNs;
    private BigDecimal thisMonthPurchaseValue = BigDecimal.ZERO;

    public ProcurementDashboardDTO() {}

    public long getTotalSuppliers() {
        return totalSuppliers;
    }

    public void setTotalSuppliers(long totalSuppliers) {
        this.totalSuppliers = totalSuppliers;
    }

    public long getActiveSuppliers() {
        return activeSuppliers;
    }

    public void setActiveSuppliers(long activeSuppliers) {
        this.activeSuppliers = activeSuppliers;
    }

    public long getDraftPOs() {
        return draftPOs;
    }

    public void setDraftPOs(long draftPOs) {
        this.draftPOs = draftPOs;
    }

    public long getPendingApprovalPOs() {
        return pendingApprovalPOs;
    }

    public void setPendingApprovalPOs(long pendingApprovalPOs) {
        this.pendingApprovalPOs = pendingApprovalPOs;
    }

    public long getOpenPurchaseOrders() {
        return openPurchaseOrders;
    }

    public void setOpenPurchaseOrders(long openPurchaseOrders) {
        this.openPurchaseOrders = openPurchaseOrders;
    }

    public long getPartiallyReceivedPOs() {
        return partiallyReceivedPOs;
    }

    public void setPartiallyReceivedPOs(long partiallyReceivedPOs) {
        this.partiallyReceivedPOs = partiallyReceivedPOs;
    }

    public long getFullyReceivedPOs() {
        return fullyReceivedPOs;
    }

    public void setFullyReceivedPOs(long fullyReceivedPOs) {
        this.fullyReceivedPOs = fullyReceivedPOs;
    }

    public long getPendingGRNs() {
        return pendingGRNs;
    }

    public void setPendingGRNs(long pendingGRNs) {
        this.pendingGRNs = pendingGRNs;
    }

    public BigDecimal getThisMonthPurchaseValue() {
        return thisMonthPurchaseValue;
    }

    public void setThisMonthPurchaseValue(BigDecimal thisMonthPurchaseValue) {
        this.thisMonthPurchaseValue = thisMonthPurchaseValue;
    }
}
