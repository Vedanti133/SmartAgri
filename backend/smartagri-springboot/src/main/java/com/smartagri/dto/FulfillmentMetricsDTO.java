package com.smartagri.dto;

public class FulfillmentMetricsDTO {
    // Orders
    private long totalOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long processingOrders;
    private long packedOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long cancelledOrders;

    // Deliveries
    private long totalDeliveries;
    private long notAssignedDeliveries;
    private long assignedDeliveries;
    private long packedDeliveries;
    private long shippedDeliveries;
    private long outForDeliveryDeliveries;
    private long deliveredDeliveries;
    private long failedDeliveries;
    private long returnedDeliveries;
    private long deliveredToday;

    // Payments
    private long pendingPayments;
    private long successfulPayments;

    public FulfillmentMetricsDTO() {}

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getConfirmedOrders() {
        return confirmedOrders;
    }

    public void setConfirmedOrders(long confirmedOrders) {
        this.confirmedOrders = confirmedOrders;
    }

    public long getProcessingOrders() {
        return processingOrders;
    }

    public void setProcessingOrders(long processingOrders) {
        this.processingOrders = processingOrders;
    }

    public long getPackedOrders() {
        return packedOrders;
    }

    public void setPackedOrders(long packedOrders) {
        this.packedOrders = packedOrders;
    }

    public long getShippedOrders() {
        return shippedOrders;
    }

    public void setShippedOrders(long shippedOrders) {
        this.shippedOrders = shippedOrders;
    }

    public long getDeliveredOrders() {
        return deliveredOrders;
    }

    public void setDeliveredOrders(long deliveredOrders) {
        this.deliveredOrders = deliveredOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public long getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(long totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    public long getNotAssignedDeliveries() {
        return notAssignedDeliveries;
    }

    public void setNotAssignedDeliveries(long notAssignedDeliveries) {
        this.notAssignedDeliveries = notAssignedDeliveries;
    }

    public long getAssignedDeliveries() {
        return assignedDeliveries;
    }

    public void setAssignedDeliveries(long assignedDeliveries) {
        this.assignedDeliveries = assignedDeliveries;
    }

    public long getPackedDeliveries() {
        return packedDeliveries;
    }

    public void setPackedDeliveries(long packedDeliveries) {
        this.packedDeliveries = packedDeliveries;
    }

    public long getShippedDeliveries() {
        return shippedDeliveries;
    }

    public void setShippedDeliveries(long shippedDeliveries) {
        this.shippedDeliveries = shippedDeliveries;
    }

    public long getOutForDeliveryDeliveries() {
        return outForDeliveryDeliveries;
    }

    public void setOutForDeliveryDeliveries(long outForDeliveryDeliveries) {
        this.outForDeliveryDeliveries = outForDeliveryDeliveries;
    }

    public long getDeliveredDeliveries() {
        return deliveredDeliveries;
    }

    public void setDeliveredDeliveries(long deliveredDeliveries) {
        this.deliveredDeliveries = deliveredDeliveries;
    }

    public long getFailedDeliveries() {
        return failedDeliveries;
    }

    public void setFailedDeliveries(long failedDeliveries) {
        this.failedDeliveries = failedDeliveries;
    }

    public long getReturnedDeliveries() {
        return returnedDeliveries;
    }

    public void setReturnedDeliveries(long returnedDeliveries) {
        this.returnedDeliveries = returnedDeliveries;
    }

    public long getDeliveredToday() {
        return deliveredToday;
    }

    public void setDeliveredToday(long deliveredToday) {
        this.deliveredToday = deliveredToday;
    }

    public long getPendingPayments() {
        return pendingPayments;
    }

    public void setPendingPayments(long pendingPayments) {
        this.pendingPayments = pendingPayments;
    }

    public long getSuccessfulPayments() {
        return successfulPayments;
    }

    public void setSuccessfulPayments(long successfulPayments) {
        this.successfulPayments = successfulPayments;
    }
}
