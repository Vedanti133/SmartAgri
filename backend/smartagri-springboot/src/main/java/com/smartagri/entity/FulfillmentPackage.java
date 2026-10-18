package com.smartagri.entity;

import com.smartagri.enums.PackageStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fulfillment_packages", indexes = {
        @Index(name = "idx_pkg_order", columnList = "order_id"),
        @Index(name = "idx_pkg_wh", columnList = "warehouse_id"),
        @Index(name = "idx_pkg_status", columnList = "package_status")
})
public class FulfillmentPackage {

    public static final BigDecimal VOLUMETRIC_DIVISOR = new BigDecimal("5000.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_number", nullable = false, unique = true, length = 50)
    private String packageNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_status", nullable = false, length = 50)
    private PackageStatus packageStatus = PackageStatus.CREATED;

    @Column(name = "actual_weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal actualWeight = BigDecimal.ZERO;

    @Column(name = "length_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal lengthCm = BigDecimal.ZERO;

    @Column(name = "width_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal widthCm = BigDecimal.ZERO;

    @Column(name = "height_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal heightCm = BigDecimal.ZERO;

    @Column(name = "volumetric_weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumetricWeight = BigDecimal.ZERO;

    @Column(name = "chargeable_weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal chargeableWeight = BigDecimal.ZERO;

    @Column(name = "packaging_type", length = 50)
    private String packagingType = "BOX";

    @Column(name = "packed_by", length = 100)
    private String packedBy;

    @Column(name = "packed_at")
    private LocalDateTime packedAt;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "courier", length = 100)
    private String courier;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "fulfillmentPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FulfillmentPackageItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public FulfillmentPackage() {}

    public void calculateWeights() {
        if (lengthCm != null && widthCm != null && heightCm != null
                && lengthCm.compareTo(BigDecimal.ZERO) > 0
                && widthCm.compareTo(BigDecimal.ZERO) > 0
                && heightCm.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal volume = lengthCm.multiply(widthCm).multiply(heightCm);
            this.volumetricWeight = volume.divide(VOLUMETRIC_DIVISOR, 2, RoundingMode.HALF_UP);
        } else {
            this.volumetricWeight = BigDecimal.ZERO;
        }

        BigDecimal actual = actualWeight != null ? actualWeight : BigDecimal.ZERO;
        this.chargeableWeight = actual.compareTo(this.volumetricWeight) >= 0 ? actual : this.volumetricWeight;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPackageNumber() { return packageNumber; }
    public void setPackageNumber(String packageNumber) { this.packageNumber = packageNumber; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public PackageStatus getPackageStatus() { return packageStatus; }
    public void setPackageStatus(PackageStatus packageStatus) { this.packageStatus = packageStatus; }

    public BigDecimal getActualWeight() { return actualWeight; }
    public void setActualWeight(BigDecimal actualWeight) { this.actualWeight = actualWeight; }

    public BigDecimal getLengthCm() { return lengthCm; }
    public void setLengthCm(BigDecimal lengthCm) { this.lengthCm = lengthCm; }

    public BigDecimal getWidthCm() { return widthCm; }
    public void setWidthCm(BigDecimal widthCm) { this.widthCm = widthCm; }

    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }

    public BigDecimal getVolumetricWeight() { return volumetricWeight; }
    public void setVolumetricWeight(BigDecimal volumetricWeight) { this.volumetricWeight = volumetricWeight; }

    public BigDecimal getChargeableWeight() { return chargeableWeight; }
    public void setChargeableWeight(BigDecimal chargeableWeight) { this.chargeableWeight = chargeableWeight; }

    public String getPackagingType() { return packagingType; }
    public void setPackagingType(String packagingType) { this.packagingType = packagingType; }

    public String getPackedBy() { return packedBy; }
    public void setPackedBy(String packedBy) { this.packedBy = packedBy; }

    public LocalDateTime getPackedAt() { return packedAt; }
    public void setPackedAt(LocalDateTime packedAt) { this.packedAt = packedAt; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getCourier() { return courier; }
    public void setCourier(String courier) { this.courier = courier; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<FulfillmentPackageItem> getItems() { return items; }
    public void setItems(List<FulfillmentPackageItem> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
