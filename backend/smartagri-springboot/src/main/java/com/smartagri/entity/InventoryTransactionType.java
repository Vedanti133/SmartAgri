package com.smartagri.entity;

public enum InventoryTransactionType {
    PURCHASE,
    SALE,
    RETURN,
    RETURN_RESTOCK,
    ADJUSTMENT_IN,
    ADJUSTMENT_OUT,
    DAMAGE,
    DAMAGED,
    CANCELLATION,
    REFUND,
    RESTOCK,
    TRANSFER_IN,
    TRANSFER_OUT,
    RETURN_IN;

    public boolean isAddition() {
        return this == PURCHASE || this == RESTOCK || this == ADJUSTMENT_IN || this == RETURN || this == RETURN_RESTOCK || this == CANCELLATION || this == TRANSFER_IN || this == RETURN_IN;
    }

    public boolean isDeduction() {
        return this == SALE || this == ADJUSTMENT_OUT || this == DAMAGE || this == DAMAGED || this == TRANSFER_OUT;
    }
}
