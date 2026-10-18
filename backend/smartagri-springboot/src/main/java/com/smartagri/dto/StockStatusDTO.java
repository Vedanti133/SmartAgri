package com.smartagri.dto;

import com.smartagri.entity.Fertilizer;
import com.smartagri.entity.StockStatus;

public class StockStatusDTO {

    private Long productId;
    private String productName;
    private String sku;
    private StockStatus status;
    private String displayBadge;
    private boolean available;
    private Integer availableStock;
    private Integer lowStockThreshold;

    public StockStatusDTO() {
    }

    public static StockStatusDTO fromFertilizer(Fertilizer f) {
        if (f == null) return null;
        StockStatusDTO dto = new StockStatusDTO();
        dto.setProductId(f.getId());
        dto.setProductName(f.getName());
        dto.setSku(f.getSku());
        StockStatus st = f.getStockStatus();
        dto.setStatus(st);
        dto.setAvailable(st != StockStatus.OUT_OF_STOCK);
        dto.setAvailableStock(f.getAvailableStock());
        dto.setLowStockThreshold(f.getLowStockThreshold());

        if (st == StockStatus.OUT_OF_STOCK) {
            dto.setDisplayBadge("Out of Stock");
        } else if (st == StockStatus.LOW_STOCK) {
            dto.setDisplayBadge("Only " + f.getAvailableStock() + " left");
        } else {
            dto.setDisplayBadge("In Stock");
        }
        return dto;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public StockStatus getStatus() {
        return status;
    }

    public void setStatus(StockStatus status) {
        this.status = status;
    }

    public String getDisplayBadge() {
        return displayBadge;
    }

    public void setDisplayBadge(String displayBadge) {
        this.displayBadge = displayBadge;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
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
}
