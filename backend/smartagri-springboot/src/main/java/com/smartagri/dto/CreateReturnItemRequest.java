package com.smartagri.dto;

import com.smartagri.enums.ItemCondition;
import com.smartagri.enums.ReturnResolution;

public class CreateReturnItemRequest {

    private Long orderItemId;
    private Integer quantity;
    private ItemCondition condition = ItemCondition.OPENED;
    private ReturnResolution resolution = ReturnResolution.REFUND;
    private String reason;

    public CreateReturnItemRequest() {}

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public ItemCondition getCondition() {
        return condition;
    }

    public void setCondition(ItemCondition condition) {
        this.condition = condition;
    }

    public ReturnResolution getResolution() {
        return resolution;
    }

    public void setResolution(ReturnResolution resolution) {
        this.resolution = resolution;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
