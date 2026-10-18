package com.smartagri.entity;

public enum PaymentStatus {
    PENDING,
    INITIATED,
    SUCCESS,
    PAID,
    FAILED,
    CANCELLED,
    REFUNDED,
    PARTIALLY_REFUNDED;

    public boolean isSuccessful() {
        return this == SUCCESS || this == PAID;
    }

    public boolean canTransitionTo(PaymentStatus next) {
        if (next == null || this == next) return true;
        return switch (this) {
            case INITIATED -> next == PENDING || next == SUCCESS || next == PAID || next == FAILED || next == CANCELLED;
            case PENDING -> next == SUCCESS || next == PAID || next == FAILED || next == CANCELLED || next == INITIATED;
            case SUCCESS, PAID -> next == REFUNDED || next == PARTIALLY_REFUNDED;
            case FAILED -> next == PENDING || next == INITIATED || next == CANCELLED;
            case CANCELLED -> false;
            case REFUNDED -> false;
            case PARTIALLY_REFUNDED -> next == REFUNDED;
        };
    }
}
