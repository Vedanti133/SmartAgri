package com.smartagri.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WishlistItemDTO {
    private Long id;
    private Long fertilizerId;
    private String name;
    private String sku;
    private String brand;
    private String imageUrl;
    private String categoryName;
    private String npkRatio;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal effectivePrice;
    private Integer availableStock;
    private boolean inStock;
    private boolean active;
    private LocalDateTime createdAt;

    public WishlistItemDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFertilizerId() { return fertilizerId; }
    public void setFertilizerId(Long fertilizerId) { this.fertilizerId = fertilizerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getNpkRatio() { return npkRatio; }
    public void setNpkRatio(String npkRatio) { this.npkRatio = npkRatio; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }

    public BigDecimal getEffectivePrice() { return effectivePrice; }
    public void setEffectivePrice(BigDecimal effectivePrice) { this.effectivePrice = effectivePrice; }

    public Integer getAvailableStock() { return availableStock; }
    public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }

    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getFertilizerName() { return name; }
    public BigDecimal getSellingPrice() { return effectivePrice != null ? effectivePrice : unitPrice; }
    public Integer getStock() { return availableStock; }
    public boolean isAvailable() { return active && inStock; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
