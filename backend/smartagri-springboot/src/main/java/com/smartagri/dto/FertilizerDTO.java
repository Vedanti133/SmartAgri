package com.smartagri.dto;

import com.smartagri.entity.Fertilizer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FertilizerDTO {

    private Long id;
    private String sku;
    private String name;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private String unit;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private Integer stock;
    private Integer minimumStock;
    private String imageUrl;
    private String suitableCrops;
    private String suitableSoil;
    private String composition;
    private String usageInstructions;
    private BigDecimal discountPercent;
    private BigDecimal taxPercent;
    private String weightValue;
    private String status;
    private Boolean active;
    private String type;
    private String npkRatio;
    private String supplier;
    private LocalDate expiryDate;
    private String description;
    private Integer lowStockThreshold;
    private Integer availableStock;
    private String stockStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FertilizerDTO() {
    }

    public static FertilizerDTO fromEntity(Fertilizer entity) {
        if (entity == null) return null;
        FertilizerDTO dto = new FertilizerDTO();
        dto.setId(entity.getId());
        dto.setSku(entity.getSku());
        dto.setName(entity.getName());
        dto.setBrand(entity.getBrand());
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
        }
        dto.setUnit(entity.getUnit());
        dto.setPurchasePrice(entity.getPurchasePrice());
        dto.setSellingPrice(entity.getSellingPrice());
        dto.setStock(entity.getStock());
        dto.setMinimumStock(entity.getMinimumStock());
        dto.setImageUrl(entity.getImageUrl());
        dto.setSuitableCrops(entity.getSuitableCrops());
        dto.setSuitableSoil(entity.getSuitableSoil());
        dto.setComposition(entity.getComposition());
        dto.setUsageInstructions(entity.getUsageInstructions());
        dto.setDiscountPercent(entity.getDiscountPercent());
        dto.setTaxPercent(entity.getTaxPercent());
        dto.setWeightValue(entity.getWeightValue());
        dto.setStatus(entity.getStatus());
        dto.setActive(entity.isActive());
        dto.setType(entity.getType());
        dto.setNpkRatio(entity.getNpkRatio());
        dto.setSupplier(entity.getSupplier());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setDescription(entity.getDescription());
        dto.setLowStockThreshold(entity.getLowStockThreshold());
        dto.setAvailableStock(entity.getAvailableStock());
        dto.setStockStatus(entity.getStockStatus() != null ? entity.getStockStatus().name() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    // Backward-compatibility getters for frontend templates
    public String getCategory() {
        return categoryName;
    }

    public BigDecimal getBuyPrice() {
        return purchasePrice;
    }

    public void setBuyPrice(BigDecimal buyPrice) {
        this.purchasePrice = buyPrice;
    }

    public BigDecimal getSellPrice() {
        return sellingPrice;
    }

    public void setSellPrice(BigDecimal sellPrice) {
        this.sellingPrice = sellPrice;
    }

    public Integer getReorderLevel() {
        return minimumStock;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.minimumStock = reorderLevel;
    }

    // Standard Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(Integer minimumStock) {
        this.minimumStock = minimumStock;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSuitableCrops() {
        return suitableCrops;
    }

    public void setSuitableCrops(String suitableCrops) {
        this.suitableCrops = suitableCrops;
    }

    public String getSuitableSoil() {
        return suitableSoil;
    }

    public void setSuitableSoil(String suitableSoil) {
        this.suitableSoil = suitableSoil;
    }

    public String getComposition() {
        return composition;
    }

    public void setComposition(String composition) {
        this.composition = composition;
    }

    public String getUsageInstructions() {
        return usageInstructions;
    }

    public void setUsageInstructions(String usageInstructions) {
        this.usageInstructions = usageInstructions;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    public BigDecimal getTaxPercent() {
        return taxPercent;
    }

    public void setTaxPercent(BigDecimal taxPercent) {
        this.taxPercent = taxPercent;
    }

    public String getWeightValue() {
        return weightValue;
    }

    public void setWeightValue(String weightValue) {
        this.weightValue = weightValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNpkRatio() {
        return npkRatio;
    }

    public void setNpkRatio(String npkRatio) {
        this.npkRatio = npkRatio;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }
}
