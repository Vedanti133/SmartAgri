package com.smartagri.dto;

public class FulfillmentDashboardDTO {
    private long ordersAwaitingFulfillment;
    private long ordersPicking;
    private long ordersPacking;
    private long ordersReadyToDispatch;
    private long shipmentsInTransit;
    private long shipmentsOutForDelivery;
    private long shipmentsDelivered;
    private long failedDeliveries;
    private long rtoShipments;
    private long totalShipments;
    private long totalPackages;

    public FulfillmentDashboardDTO() {}

    public long getOrdersAwaitingFulfillment() { return ordersAwaitingFulfillment; }
    public void setOrdersAwaitingFulfillment(long ordersAwaitingFulfillment) { this.ordersAwaitingFulfillment = ordersAwaitingFulfillment; }

    public long getOrdersPicking() { return ordersPicking; }
    public void setOrdersPicking(long ordersPicking) { this.ordersPicking = ordersPicking; }

    public long getOrdersPacking() { return ordersPacking; }
    public void setOrdersPacking(long ordersPacking) { this.ordersPacking = ordersPacking; }

    public long getOrdersReadyToDispatch() { return ordersReadyToDispatch; }
    public void setOrdersReadyToDispatch(long ordersReadyToDispatch) { this.ordersReadyToDispatch = ordersReadyToDispatch; }

    public long getShipmentsInTransit() { return shipmentsInTransit; }
    public void setShipmentsInTransit(long shipmentsInTransit) { this.shipmentsInTransit = shipmentsInTransit; }

    public long getShipmentsOutForDelivery() { return shipmentsOutForDelivery; }
    public void setShipmentsOutForDelivery(long shipmentsOutForDelivery) { this.shipmentsOutForDelivery = shipmentsOutForDelivery; }

    public long getShipmentsDelivered() { return shipmentsDelivered; }
    public void setShipmentsDelivered(long shipmentsDelivered) { this.shipmentsDelivered = shipmentsDelivered; }

    public long getFailedDeliveries() { return failedDeliveries; }
    public void setFailedDeliveries(long failedDeliveries) { this.failedDeliveries = failedDeliveries; }

    public long getRtoShipments() { return rtoShipments; }
    public void setRtoShipments(long rtoShipments) { this.rtoShipments = rtoShipments; }

    public long getTotalShipments() { return totalShipments; }
    public void setTotalShipments(long totalShipments) { this.totalShipments = totalShipments; }

    public long getTotalPackages() { return totalPackages; }
    public void setTotalPackages(long totalPackages) { this.totalPackages = totalPackages; }

    public long getDeliveredCount() { return shipmentsDelivered; }
    public long getPickingPackingCount() { return ordersPicking + ordersPacking; }
    public long getAwaitingAllocationCount() { return ordersAwaitingFulfillment; }
    public long getInTransitOutForDeliveryCount() { return shipmentsInTransit + shipmentsOutForDelivery; }
}
