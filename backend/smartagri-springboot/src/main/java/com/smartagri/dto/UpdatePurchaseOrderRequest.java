package com.smartagri.dto;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

public class UpdatePurchaseOrderRequest {

    private Long supplierId;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String notes;

    @Valid
    private List<CreatePurchaseOrderItemRequest> items;

    public UpdatePurchaseOrderRequest() {}

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<CreatePurchaseOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreatePurchaseOrderItemRequest> items) {
        this.items = items;
    }
}
