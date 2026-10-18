package com.smartagri.dto;

import com.smartagri.entity.LocationStatus;
import com.smartagri.entity.LocationType;
import com.smartagri.entity.WarehouseLocation;

import java.time.LocalDateTime;

public class WarehouseLocationDTO {

    private Long id;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String locationCode;
    private LocationType locationType;
    private Long parentLocationId;
    private String parentLocationCode;
    private String name;
    private Integer capacity;
    private Integer currentUtilization;
    private LocationStatus status;
    private Double maxWeightKg;
    private Boolean isTemperatureControlled = false;
    private Boolean isHazardousAllowed = false;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WarehouseLocationDTO() {
    }

    public static WarehouseLocationDTO fromEntity(WarehouseLocation l) {
        return fromEntity(l, 0);
    }

    public static WarehouseLocationDTO fromEntity(WarehouseLocation l, int utilization) {
        if (l == null) return null;
        WarehouseLocationDTO dto = new WarehouseLocationDTO();
        dto.setId(l.getId());
        if (l.getWarehouse() != null) {
            dto.setWarehouseId(l.getWarehouse().getId());
            dto.setWarehouseCode(l.getWarehouse().getWarehouseCode());
            dto.setWarehouseName(l.getWarehouse().getWarehouseName());
        }
        dto.setLocationCode(l.getLocationCode());
        dto.setLocationType(l.getLocationType());
        if (l.getParentLocation() != null) {
            dto.setParentLocationId(l.getParentLocation().getId());
            dto.setParentLocationCode(l.getParentLocation().getLocationCode());
        }
        dto.setName(l.getName());
        dto.setCapacity(l.getCapacity());
        dto.setCurrentUtilization(utilization);
        dto.setStatus(l.getStatus());
        dto.setMaxWeightKg(l.getMaxWeightKg());
        dto.setIsTemperatureControlled(l.getIsTemperatureControlled() != null ? l.getIsTemperatureControlled() : false);
        dto.setIsHazardousAllowed(l.getIsHazardousAllowed() != null ? l.getIsHazardousAllowed() : false);
        dto.setNotes(l.getNotes());
        dto.setCreatedAt(l.getCreatedAt());
        dto.setUpdatedAt(l.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
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

    public Long getParentLocationId() {
        return parentLocationId;
    }

    public void setParentLocationId(Long parentLocationId) {
        this.parentLocationId = parentLocationId;
    }

    public String getParentLocationCode() {
        return parentLocationCode;
    }

    public void setParentLocationCode(String parentLocationCode) {
        this.parentLocationCode = parentLocationCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getCurrentUtilization() {
        return currentUtilization;
    }

    public void setCurrentUtilization(Integer currentUtilization) {
        this.currentUtilization = currentUtilization;
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

    public String getLocationName() {
        return name;
    }

    public void setLocationName(String locationName) {
        this.name = locationName;
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
