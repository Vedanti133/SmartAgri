package com.smartagri.dto;

public class OrderMetricsDTO {
    private long totalOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long processingOrders;
    private long packedOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long cancelledOrders;

    public OrderMetricsDTO() {}

    public OrderMetricsDTO(long totalOrders, long pendingOrders, long confirmedOrders, long processingOrders,
                           long packedOrders, long shippedOrders, long deliveredOrders, long cancelledOrders) {
        this.totalOrders = totalOrders;
        this.pendingOrders = pendingOrders;
        this.confirmedOrders = confirmedOrders;
        this.processingOrders = processingOrders;
        this.packedOrders = packedOrders;
        this.shippedOrders = shippedOrders;
        this.deliveredOrders = deliveredOrders;
        this.cancelledOrders = cancelledOrders;
    }

    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }

    public long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }

    public long getConfirmedOrders() { return confirmedOrders; }
    public void setConfirmedOrders(long confirmedOrders) { this.confirmedOrders = confirmedOrders; }

    public long getProcessingOrders() { return processingOrders; }
    public void setProcessingOrders(long processingOrders) { this.processingOrders = processingOrders; }

    public long getPackedOrders() { return packedOrders; }
    public void setPackedOrders(long packedOrders) { this.packedOrders = packedOrders; }

    public long getShippedOrders() { return shippedOrders; }
    public void setShippedOrders(long shippedOrders) { this.shippedOrders = shippedOrders; }

    public long getDeliveredOrders() { return deliveredOrders; }
    public void setDeliveredOrders(long deliveredOrders) { this.deliveredOrders = deliveredOrders; }

    public long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(long cancelledOrders) { this.cancelledOrders = cancelledOrders; }
}
