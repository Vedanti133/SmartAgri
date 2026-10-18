package com.smartagri.dto;

public class WarehouseDashboardDTO {

    private long totalWarehouses;
    private long activeWarehouses;
    private long totalLocations;
    private long occupiedLocations;
    private long totalInventoryUnits;
    private long lowStockProducts;
    private long nearExpiryBatches;
    private long expiredBatches;
    private long pendingTransfers;
    private long transfersInTransit;

    public WarehouseDashboardDTO() {
    }

    public WarehouseDashboardDTO(long totalWarehouses, long activeWarehouses, long totalLocations, long occupiedLocations,
                                 long totalInventoryUnits, long lowStockProducts, long nearExpiryBatches,
                                 long expiredBatches, long pendingTransfers, long transfersInTransit) {
        this.totalWarehouses = totalWarehouses;
        this.activeWarehouses = activeWarehouses;
        this.totalLocations = totalLocations;
        this.occupiedLocations = occupiedLocations;
        this.totalInventoryUnits = totalInventoryUnits;
        this.lowStockProducts = lowStockProducts;
        this.nearExpiryBatches = nearExpiryBatches;
        this.expiredBatches = expiredBatches;
        this.pendingTransfers = pendingTransfers;
        this.transfersInTransit = transfersInTransit;
    }

    public long getTotalWarehouses() {
        return totalWarehouses;
    }

    public void setTotalWarehouses(long totalWarehouses) {
        this.totalWarehouses = totalWarehouses;
    }

    public long getActiveWarehouses() {
        return activeWarehouses;
    }

    public void setActiveWarehouses(long activeWarehouses) {
        this.activeWarehouses = activeWarehouses;
    }

    public long getTotalLocations() {
        return totalLocations;
    }

    public void setTotalLocations(long totalLocations) {
        this.totalLocations = totalLocations;
    }

    public long getOccupiedLocations() {
        return occupiedLocations;
    }

    public void setOccupiedLocations(long occupiedLocations) {
        this.occupiedLocations = occupiedLocations;
    }

    public long getTotalInventoryUnits() {
        return totalInventoryUnits;
    }

    public void setTotalInventoryUnits(long totalInventoryUnits) {
        this.totalInventoryUnits = totalInventoryUnits;
    }

    public long getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(long lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }

    public long getNearExpiryBatches() {
        return nearExpiryBatches;
    }

    public void setNearExpiryBatches(long nearExpiryBatches) {
        this.nearExpiryBatches = nearExpiryBatches;
    }

    public long getExpiredBatches() {
        return expiredBatches;
    }

    public void setExpiredBatches(long expiredBatches) {
        this.expiredBatches = expiredBatches;
    }

    public long getPendingTransfers() {
        return pendingTransfers;
    }

    public void setPendingTransfers(long pendingTransfers) {
        this.pendingTransfers = pendingTransfers;
    }

    public long getTransfersInTransit() {
        return transfersInTransit;
    }

    public void setTransfersInTransit(long transfersInTransit) {
        this.transfersInTransit = transfersInTransit;
    }
}
