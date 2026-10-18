package com.smartagri.dto;

import com.smartagri.entity.Fertilizer;
import com.smartagri.entity.StockStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryDTO {

    private Long productId;
    private String sku;
    private String name;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private String unit;
    private Integer currentStock;
    private Integer reservedStock;
    private Integer availableStock;
    private Integer lowStockThreshold;
    private StockStatus status;
    private String statusDisplay;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private LocalDateTime lastUpdated;

    public InventoryDTO() {
    }

    public static InventoryDTO fromFertilizer(Fertilizer f) {
        if (f == null) return null;
        InventoryDTO dto = new InventoryDTO();
        dto.setProductId(f.getId());
        dto.setSku(f.getSku());
        dto.setName(f.getName());
        dto.setBrand(f.getBrand());
        if (f.getCategory() != null) {
            dto.setCategoryId(f.getCategory().getId());
            dto.setCategoryName(f.getCategory().getName());
        }
        dto.setUnit(f.getUnit());
        dto.setCurrentStock(f.getStock() != null ? f.getStock() : 0);
        dto.setReservedStock(f.getReservedStock() != null ? f.getReservedStock() : 0);
        dto.setAvailableStock(f.getAvailableStock());
        dto.setLowStockThreshold(f.getLowStockThreshold());
        dto.setStatus(f.getStockStatus());
        dto.setStatusDisplay(f.getStockStatus().getDisplayBadge());
        dto.setPurchasePrice(f.getPurchasePrice());
        dto.setSellingPrice(f.getSellingPrice());
        dto.setLastUpdated(f.getUpdatedAt());
        return dto;
    }

    // Getters and Setters

    public Long getProductId() {
        return productId;
    }

    public Long getId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getStock() {
        return currentStock;
    }

    public String getStockStatus() {
        return status != null ? status.name() : null;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public Integer getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(Integer reservedStock) {
        this.reservedStock = reservedStock;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public StockStatus getStatus() {
        return status;
    }

    public void setStatus(StockStatus status) {
        this.status = status;
    }

    public String getStatusDisplay() {
        return statusDisplay;
    }

    public void setStatusDisplay(String statusDisplay) {
        this.statusDisplay = statusDisplay;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
