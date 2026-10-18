package com.smartagri.enums;

public enum ShipmentStatus {
    CREATED,
    READY_FOR_DISPATCH,
    DISPATCHED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELIVERY_FAILED,
    RETURN_TO_ORIGIN,
    RETURNED,
    CANCELLED;

    public boolean canTransitionTo(ShipmentStatus next) {
        if (this == next) return true;
        return switch (this) {
            case CREATED -> next == READY_FOR_DISPATCH || next == DISPATCHED || next == CANCELLED;
            case READY_FOR_DISPATCH -> next == DISPATCHED || next == CANCELLED;
            case DISPATCHED -> next == IN_TRANSIT || next == OUT_FOR_DELIVERY || next == DELIVERY_FAILED || next == DELIVERED;
            case IN_TRANSIT -> next == OUT_FOR_DELIVERY || next == DELIVERY_FAILED || next == RETURN_TO_ORIGIN;
            case OUT_FOR_DELIVERY -> next == DELIVERED || next == DELIVERY_FAILED || next == RETURN_TO_ORIGIN;
            case DELIVERY_FAILED -> next == OUT_FOR_DELIVERY || next == RETURN_TO_ORIGIN || next == DELIVERED;
            case RETURN_TO_ORIGIN -> next == RETURNED;
            case DELIVERED, RETURNED, CANCELLED -> false;
        };
    }
}
