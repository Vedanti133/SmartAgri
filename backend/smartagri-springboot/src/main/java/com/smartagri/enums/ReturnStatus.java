package com.smartagri.enums;

public enum ReturnStatus {
    REQUESTED,
    UNDER_REVIEW,
    APPROVED,
    PICKUP_SCHEDULED,
    PICKED_UP,
    IN_TRANSIT,
    RECEIVED,
    INSPECTING,
    INSPECTION_COMPLETED,
    REFUND_PENDING,
    REFUNDED,
    REPLACEMENT_PENDING,
    REPLACED,
    REJECTED,
    CANCELLED,
    FAILED_PICKUP,
    RETURNED_TO_CUSTOMER;

    public boolean canCancel() {
        return this == REQUESTED || this == UNDER_REVIEW;
    }

    public boolean isTerminal() {
        return this == REFUNDED || this == REPLACED || this == REJECTED || this == CANCELLED || this == RETURNED_TO_CUSTOMER;
    }
}
