package com.smartagri.dto;

import com.smartagri.entity.Category;
import java.time.LocalDateTime;

public class CategoryDTO {

    private Long id;
    private String name;
    private String description;
    private String status;
    private Long productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CategoryDTO() {
    }

    public CategoryDTO(Long id, String name, String description, String status, Long productCount,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.productCount = productCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CategoryDTO fromEntity(Category category, long productCount) {
        if (category == null) return null;
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getStatus(),
                productCount,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getProductCount() {
        return productCount;
    }

    public void setProductCount(Long productCount) {
        this.productCount = productCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
