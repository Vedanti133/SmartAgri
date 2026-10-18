package com.smartagri.entity;

public enum StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK;

    public String getDisplayBadge() {
        return switch (this) {
            case IN_STOCK -> "In Stock";
            case LOW_STOCK -> "Low Stock";
            case OUT_OF_STOCK -> "Out of Stock";
        };
    }
}
