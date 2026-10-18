package com.smartagri.dto;

import java.util.List;

public class ConfirmPickRequest {

    private Long itemId;
    private Integer pickedQuantity;
    private List<PickItemConfirmation> items;

    public ConfirmPickRequest() {}

    public static class PickItemConfirmation {
        private Long itemId;
        private Integer pickedQuantity;

        public PickItemConfirmation() {}

        public PickItemConfirmation(Long itemId, Integer pickedQuantity) {
            this.itemId = itemId;
            this.pickedQuantity = pickedQuantity;
        }

        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }

        public Long getPickListItemId() { return itemId; }
        public void setPickListItemId(Long pickListItemId) { this.itemId = pickListItemId; }

        public Integer getPickedQuantity() { return pickedQuantity; }
        public void setPickedQuantity(Integer pickedQuantity) { this.pickedQuantity = pickedQuantity; }

        public Integer getQuantityPicked() { return pickedQuantity; }
        public void setQuantityPicked(Integer quantityPicked) { this.pickedQuantity = quantityPicked; }
    }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Integer getPickedQuantity() { return pickedQuantity; }
    public void setPickedQuantity(Integer pickedQuantity) { this.pickedQuantity = pickedQuantity; }

    public List<PickItemConfirmation> getItems() { return items; }
    public void setItems(List<PickItemConfirmation> items) { this.items = items; }

    public List<PickItemConfirmation> getItemPicks() { return items; }
    public void setItemPicks(List<PickItemConfirmation> itemPicks) { this.items = itemPicks; }
}
