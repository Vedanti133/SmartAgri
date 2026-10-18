package com.smartagri.dto;

import java.util.ArrayList;
import java.util.List;

public class WishlistDTO {
    private Long id;
    private Long userId;
    private List<WishlistItemDTO> items = new ArrayList<>();
    private int totalItemsCount = 0;

    public WishlistDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public List<WishlistItemDTO> getItems() { return items; }
    public void setItems(List<WishlistItemDTO> items) { this.items = items; }

    public int getTotalItemsCount() { return totalItemsCount; }
    public void setTotalItemsCount(int totalItemsCount) { this.totalItemsCount = totalItemsCount; }
}
