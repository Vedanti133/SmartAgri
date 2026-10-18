package com.smartagri.entity;

public enum DeliveryStatus {
    NOT_ASSIGNED,
    ASSIGNED,
    PACKED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    RETURNED;

    public boolean canTransitionTo(DeliveryStatus next) {
        if (this == next) return true;
        return switch (this) {
            case NOT_ASSIGNED -> next == ASSIGNED;
            case ASSIGNED -> next == PACKED || next == SHIPPED || next == FAILED;
            case PACKED -> next == SHIPPED || next == FAILED;
            case SHIPPED -> next == OUT_FOR_DELIVERY || next == FAILED || next == DELIVERED;
            case OUT_FOR_DELIVERY -> next == DELIVERED || next == FAILED;
            case FAILED -> next == OUT_FOR_DELIVERY || next == RETURNED || next == ASSIGNED;
            case DELIVERED -> next == RETURNED;
            case RETURNED -> false;
        };
    }
}
