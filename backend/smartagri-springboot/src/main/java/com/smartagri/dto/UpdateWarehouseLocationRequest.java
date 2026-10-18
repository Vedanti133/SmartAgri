package com.smartagri.dto;

import com.smartagri.entity.LocationType;

public class UpdateWarehouseLocationRequest {

    private String locationCode;
    private LocationType locationType;
    private Long parentLocationId;
    private String name;
    private String locationName;
    private Integer capacity;
    private Double maxWeightKg;
    private Boolean isTemperatureControlled;
    private Boolean isHazardousAllowed;
    private String notes;

    public UpdateWarehouseLocationRequest() {
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

    public String getName() {
        if (name != null && !name.isBlank()) return name;
        if (locationName != null && !locationName.isBlank()) return locationName;
        return null;
    }

    public void setName(String name) {
        this.name = name;
        if (this.locationName == null) this.locationName = name;
    }

    public String getLocationName() {
        return getName();
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
        if (this.name == null) this.name = locationName;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
