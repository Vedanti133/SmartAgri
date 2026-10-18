package com.smartagri.dto;

public class InventorySummaryDTO {

    private long totalProducts;
    private long totalStockUnits;
    private long lowStockCount;
    private long outOfStockCount;
    private long stockAddedToday;
    private long stockRemovedToday;

    public InventorySummaryDTO() {
    }

    public InventorySummaryDTO(long totalProducts, long totalStockUnits, long lowStockCount, long outOfStockCount, long stockAddedToday, long stockRemovedToday) {
        this.totalProducts = totalProducts;
        this.totalStockUnits = totalStockUnits;
        this.lowStockCount = lowStockCount;
        this.outOfStockCount = outOfStockCount;
        this.stockAddedToday = stockAddedToday;
        this.stockRemovedToday = stockRemovedToday;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalStockUnits() {
        return totalStockUnits;
    }

    public void setTotalStockUnits(long totalStockUnits) {
        this.totalStockUnits = totalStockUnits;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public long getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(long outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public long getStockAddedToday() {
        return stockAddedToday;
    }

    public void setStockAddedToday(long stockAddedToday) {
        this.stockAddedToday = stockAddedToday;
    }

    public long getStockRemovedToday() {
        return stockRemovedToday;
    }

    public void setStockRemovedToday(long stockRemovedToday) {
        this.stockRemovedToday = stockRemovedToday;
    }
}
