package com.smartagri.dto;

import java.util.ArrayList;
import java.util.List;

public class ReceiveStockTransferRequest {

    private String notes;
    private List<ItemReceipt> itemReceipts = new ArrayList<>();

    public ReceiveStockTransferRequest() {
    }

    public static class ItemReceipt {
        private Long itemId;
        private Integer receivedQuantity;
        private Long destinationLocationId;

        public ItemReceipt() {
        }

        public ItemReceipt(Long itemId, Integer receivedQuantity) {
            this.itemId = itemId;
            this.receivedQuantity = receivedQuantity;
        }

        public ItemReceipt(Long itemId, Integer receivedQuantity, Long destinationLocationId) {
            this.itemId = itemId;
            this.receivedQuantity = receivedQuantity;
            this.destinationLocationId = destinationLocationId;
        }

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Integer getReceivedQuantity() {
            return receivedQuantity;
        }

        public void setReceivedQuantity(Integer receivedQuantity) {
            this.receivedQuantity = receivedQuantity;
        }

        public Long getDestinationLocationId() {
            return destinationLocationId;
        }

        public void setDestinationLocationId(Long destinationLocationId) {
            this.destinationLocationId = destinationLocationId;
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ItemReceipt> getItemReceipts() {
        return itemReceipts;
    }

    public void setItemReceipts(List<ItemReceipt> itemReceipts) {
        this.itemReceipts = itemReceipts;
    }

    public List<ItemReceipt> getItems() {
        return itemReceipts;
    }

    public void setItems(List<ItemReceipt> items) {
        this.itemReceipts = items;
    }
}
