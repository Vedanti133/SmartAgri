package com.smartagri.dto;

import com.smartagri.enums.ReturnReason;
import java.util.ArrayList;
import java.util.List;

public class CreateReturnRequest {

    private Long orderId;
    private ReturnReason reason;
    private String customerComments;
    private String pickupAddress;
    private String bankAccountDetails;
    private List<CreateReturnItemRequest> items = new ArrayList<>();

    public CreateReturnRequest() {}

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public ReturnReason getReason() {
        return reason;
    }

    public void setReason(ReturnReason reason) {
        this.reason = reason;
    }

    public String getCustomerComments() {
        return customerComments;
    }

    public void setCustomerComments(String customerComments) {
        this.customerComments = customerComments;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getBankAccountDetails() {
        return bankAccountDetails;
    }

    public void setBankAccountDetails(String bankAccountDetails) {
        this.bankAccountDetails = bankAccountDetails;
    }

    public List<CreateReturnItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreateReturnItemRequest> items) {
        this.items = items;
    }
}
