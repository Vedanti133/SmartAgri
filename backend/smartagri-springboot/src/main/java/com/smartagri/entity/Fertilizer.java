package com.smartagri.entity;

import com.smartagri.enums.ProductReturnPolicy;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fertilizer")
public class Fertilizer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_policy", length = 30)
    private ProductReturnPolicy returnPolicy = ProductReturnPolicy.RETURNABLE;

    @Column(length = 100)
    private String brand;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 30)
    private String unit;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "selling_price", precision = 10, scale = 2)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "reserved_stock", nullable = false)
    private Integer reservedStock = 0;

    @Column(name = "minimum_stock")
    private Integer minimumStock = 10;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 10;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "suitable_crops", length = 500)
    private String suitableCrops;

    @Column(name = "suitable_soil", length = 500)
    private String suitableSoil;

    @Column(length = 500)
    private String composition;

    @Column(name = "usage_instructions", columnDefinition = "TEXT")
    private String usageInstructions;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent = new BigDecimal("5.00");

    @Column(name = "weight_value", length = 50)
    private String weightValue;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 100)
    private String type;

    @Column(name = "npk_ratio", length = 50)
    private String npkRatio;

    @Column(length = 150)
    private String supplier;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Fertilizer() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        syncStatusAndStock();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        syncStatusAndStock();
    }

    public void syncStatusAndStock() {
        if (this.stock == null || this.stock < 0) {
            this.stock = 0;
        }
        if (this.reservedStock == null || this.reservedStock < 0) {
            this.reservedStock = 0;
        }
        if (this.lowStockThreshold == null || this.lowStockThreshold < 0) {
            this.lowStockThreshold = this.minimumStock != null ? this.minimumStock : 10;
        }
        if (this.stock == 0 && !"INACTIVE".equalsIgnoreCase(this.status)) {
            this.status = "OUT_OF_STOCK";
        } else if (this.stock > 0 && "OUT_OF_STOCK".equalsIgnoreCase(this.status)) {
            this.status = "ACTIVE";
        } else if (this.status == null || this.status.isBlank()) {
            this.status = this.stock > 0 ? "ACTIVE" : "OUT_OF_STOCK";
        }
        this.active = !"INACTIVE".equalsIgnoreCase(this.status);
    }

    public int getAvailableStock() {
        int current = this.stock != null ? this.stock : 0;
        int reserved = this.reservedStock != null ? this.reservedStock : 0;
        return Math.max(0, current - reserved);
    }

    public StockStatus getStockStatus() {
        int available = getAvailableStock();
        if (available <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        int threshold = this.lowStockThreshold != null ? this.lowStockThreshold : (this.minimumStock != null ? this.minimumStock : 10);
        if (available <= threshold) {
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.IN_STOCK;
    }

    // Getters and Setters

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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getCategoryName() {
        return category != null ? category.getName() : null;
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
        this.active = !"INACTIVE".equalsIgnoreCase(status);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            this.status = "INACTIVE";
        } else if ("INACTIVE".equalsIgnoreCase(this.status)) {
            this.status = (stock != null && stock > 0) ? "ACTIVE" : "OUT_OF_STOCK";
        }
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

    public Integer getReservedStock() {
        return reservedStock != null ? reservedStock : 0;
    }

    public void setReservedStock(Integer reservedStock) {
        this.reservedStock = reservedStock != null ? Math.max(0, reservedStock) : 0;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold != null ? lowStockThreshold : (minimumStock != null ? minimumStock : 10);
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold != null ? Math.max(0, lowStockThreshold) : 10;
        this.minimumStock = this.lowStockThreshold;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ProductReturnPolicy getReturnPolicy() {
        return returnPolicy != null ? returnPolicy : ProductReturnPolicy.RETURNABLE;
    }

    public void setReturnPolicy(ProductReturnPolicy returnPolicy) {
        this.returnPolicy = returnPolicy != null ? returnPolicy : ProductReturnPolicy.RETURNABLE;
    }
}
