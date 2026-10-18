package com.smartagri.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_locations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wh_loc", columnNames = {"warehouse_id", "location_code"})
        },
        indexes = {
                @Index(name = "idx_wl_warehouse", columnList = "warehouse_id"),
                @Index(name = "idx_wl_code", columnList = "location_code"),
                @Index(name = "idx_wl_parent", columnList = "parent_location_id"),
                @Index(name = "idx_wl_status", columnList = "status"),
                @Index(name = "idx_wl_type", columnList = "location_type")
        }
)
public class WarehouseLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "location_code", nullable = false, length = 50)
    private String locationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 30)
    private LocationType locationType = LocationType.BIN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_location_id")
    private WarehouseLocation parentLocation;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer capacity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LocationStatus status = LocationStatus.ACTIVE;

    @Column(name = "max_weight_kg")
    private Double maxWeightKg;

    @Column(name = "is_temperature_controlled")
    private Boolean isTemperatureControlled = false;

    @Column(name = "is_hazardous_allowed")
    private Boolean isHazardousAllowed = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WarehouseLocation() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.locationType == null) {
            this.locationType = LocationType.BIN;
        }
        if (this.status == null) {
            this.status = LocationStatus.ACTIVE;
        }
        if (this.capacity == null) {
            this.capacity = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == LocationStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return this.status == LocationStatus.BLOCKED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    public WarehouseLocation getParentLocation() {
        return parentLocation;
    }

    public void setParentLocation(WarehouseLocation parentLocation) {
        this.parentLocation = parentLocation;
    }

    public String getName() {
        return name;
    }

    public String getLocationName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocationName(String locationName) {
        this.name = locationName;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public LocationStatus getStatus() {
        return status;
    }

    public void setStatus(LocationStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public Double getMaxWeightKg() {
        return maxWeightKg;
    }

    public void setMaxWeightKg(Double maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }

    public Boolean getIsTemperatureControlled() {
        return isTemperatureControlled;
    }

    public void setIsTemperatureControlled(Boolean temperatureControlled) {
        isTemperatureControlled = temperatureControlled;
    }

    public Boolean getIsHazardousAllowed() {
        return isHazardousAllowed;
    }

    public void setIsHazardousAllowed(Boolean hazardousAllowed) {
        isHazardousAllowed = hazardousAllowed;
    }
}
