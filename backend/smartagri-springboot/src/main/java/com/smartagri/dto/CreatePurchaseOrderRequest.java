package com.smartagri.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class CreatePurchaseOrderRequest {

    @NotNull(message = "Supplier is required")
    private Long supplierId;

    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String notes;

    @NotEmpty(message = "At least one item is required in the purchase order")
    @Valid
    private List<CreatePurchaseOrderItemRequest> items;

    public CreatePurchaseOrderRequest() {}

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
