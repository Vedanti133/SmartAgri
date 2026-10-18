package com.smartagri.dto;

import com.smartagri.entity.Warehouse;
import com.smartagri.entity.WarehouseStatus;
import com.smartagri.entity.WarehouseType;

import java.time.LocalDateTime;

public class WarehouseDTO {

    private Long id;
    private String warehouseCode;
    private String warehouseName;
    private WarehouseType warehouseType;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String contactPerson;
    private String contactPhone;
    private String email;
    private WarehouseStatus status;
    private Integer capacity;
    private Integer currentUtilization;
    private Boolean isDefaultReceiving;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WarehouseDTO() {
    }

    public static WarehouseDTO fromEntity(Warehouse w) {
        return fromEntity(w, 0);
    }

    public static WarehouseDTO fromEntity(Warehouse w, int utilization) {
        if (w == null) return null;
        WarehouseDTO dto = new WarehouseDTO();
        dto.setId(w.getId());
        dto.setWarehouseCode(w.getWarehouseCode());
        dto.setWarehouseName(w.getWarehouseName());
        dto.setWarehouseType(w.getWarehouseType());
        dto.setAddress(w.getAddress());
        dto.setCity(w.getCity());
        dto.setState(w.getState());
        dto.setPostalCode(w.getPostalCode());
        dto.setContactPerson(w.getContactPerson());
        dto.setContactPhone(w.getContactPhone());
        dto.setEmail(w.getEmail());
        dto.setStatus(w.getStatus());
        dto.setCapacity(w.getCapacity());
        dto.setCurrentUtilization(utilization);
        dto.setIsDefaultReceiving(w.getIsDefaultReceiving());
        dto.setNotes(w.getNotes());
        dto.setCreatedAt(w.getCreatedAt());
        dto.setUpdatedAt(w.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public WarehouseType getWarehouseType() {
        return warehouseType;
    }

    public void setWarehouseType(WarehouseType warehouseType) {
        this.warehouseType = warehouseType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public WarehouseStatus getStatus() {
        return status;
    }

    public void setStatus(WarehouseStatus status) {
        this.status = status;
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

    public Boolean getIsDefaultReceiving() {
        return isDefaultReceiving;
    }

    public void setIsDefaultReceiving(Boolean defaultReceiving) {
        isDefaultReceiving = defaultReceiving;
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
}
