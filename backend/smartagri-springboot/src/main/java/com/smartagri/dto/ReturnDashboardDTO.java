package com.smartagri.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReturnDashboardDTO {

    private long totalReturns;
    private long pendingReturns;
    private long approvedReturns;
    private long inInspection;
    private long completedReturns;
    private long rejectedReturns;
    private BigDecimal totalRefundedAmount = BigDecimal.ZERO;
    private BigDecimal pendingRefundAmount = BigDecimal.ZERO;
    private long openTicketsCount;
    private List<ReturnRequestDTO> recentReturns = new ArrayList<>();
    private List<SupportTicketDTO> recentTickets = new ArrayList<>();

    public ReturnDashboardDTO() {}

    public long getTotalReturns() {
        return totalReturns;
    }

    public void setTotalReturns(long totalReturns) {
        this.totalReturns = totalReturns;
    }

    public long getPendingReturns() {
        return pendingReturns;
    }

    public void setPendingReturns(long pendingReturns) {
        this.pendingReturns = pendingReturns;
    }

    public long getApprovedReturns() {
        return approvedReturns;
    }

    public void setApprovedReturns(long approvedReturns) {
        this.approvedReturns = approvedReturns;
    }

    public long getInInspection() {
        return inInspection;
    }

    public void setInInspection(long inInspection) {
        this.inInspection = inInspection;
    }

    public long getCompletedReturns() {
        return completedReturns;
    }

    public void setCompletedReturns(long completedReturns) {
        this.completedReturns = completedReturns;
    }

    public long getRejectedReturns() {
        return rejectedReturns;
    }

    public void setRejectedReturns(long rejectedReturns) {
        this.rejectedReturns = rejectedReturns;
    }

    public BigDecimal getTotalRefundedAmount() {
        return totalRefundedAmount;
    }

    public void setTotalRefundedAmount(BigDecimal totalRefundedAmount) {
        this.totalRefundedAmount = totalRefundedAmount;
    }

    public BigDecimal getPendingRefundAmount() {
        return pendingRefundAmount;
    }

    public void setPendingRefundAmount(BigDecimal pendingRefundAmount) {
        this.pendingRefundAmount = pendingRefundAmount;
    }

    public long getOpenTicketsCount() {
        return openTicketsCount;
    }

    public void setOpenTicketsCount(long openTicketsCount) {
        this.openTicketsCount = openTicketsCount;
    }

    public List<ReturnRequestDTO> getRecentReturns() {
        return recentReturns;
    }

    public void setRecentReturns(List<ReturnRequestDTO> recentReturns) {
        this.recentReturns = recentReturns;
    }

    public List<SupportTicketDTO> getRecentTickets() {
        return recentTickets;
    }

    public void setRecentTickets(List<SupportTicketDTO> recentTickets) {
        this.recentTickets = recentTickets;
    }
}
