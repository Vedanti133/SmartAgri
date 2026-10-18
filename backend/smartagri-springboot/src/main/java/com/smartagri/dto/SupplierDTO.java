package com.smartagri.dto;

import com.smartagri.entity.Supplier;
import com.smartagri.entity.SupplierStatus;

import java.time.LocalDateTime;

public class SupplierDTO {

    private Long id;
    private String supplierCode;
    private String companyName;
    private String contactPerson;
    private String email;
    private String phone;
    private String alternatePhone;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String gstNumber;
    private String panNumber;
    private String paymentTerms;
    private SupplierStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SupplierDTO() {}

    public static SupplierDTO fromEntity(Supplier s) {
        if (s == null) return null;
        SupplierDTO dto = new SupplierDTO();
        dto.setId(s.getId());
        dto.setSupplierCode(s.getSupplierCode());
        dto.setCompanyName(s.getCompanyName());
        dto.setContactPerson(s.getContactPerson());
        dto.setEmail(s.getEmail());
        dto.setPhone(s.getPhone());
        dto.setAlternatePhone(s.getAlternatePhone());
        dto.setAddress(s.getAddress());
        dto.setCity(s.getCity());
        dto.setState(s.getState());
        dto.setPostalCode(s.getPostalCode());
        dto.setGstNumber(s.getGstNumber());
        dto.setPanNumber(s.getPanNumber());
        dto.setPaymentTerms(s.getPaymentTerms());
        dto.setStatus(s.getStatus());
        dto.setNotes(s.getNotes());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAlternatePhone() {
        return alternatePhone;
    }

    public void setAlternatePhone(String alternatePhone) {
        this.alternatePhone = alternatePhone;
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

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public SupplierStatus getStatus() {
        return status;
    }

    public void setStatus(SupplierStatus status) {
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
}
