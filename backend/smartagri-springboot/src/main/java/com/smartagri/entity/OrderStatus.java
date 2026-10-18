package com.smartagri.entity;

import java.util.Set;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    PACKED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canCancel() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean canTransitionTo(OrderStatus next) {
        if (this == next) return true;
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == PROCESSING || next == CANCELLED;
            case PROCESSING -> next == PACKED;
            case PACKED -> next == SHIPPED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
