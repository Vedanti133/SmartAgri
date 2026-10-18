package com.smartagri.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FertilizerFilterOptionsDTO {

    private List<CategoryDTO> categories = new ArrayList<>();
    private List<String> brands = new ArrayList<>();
    private List<String> fertilizerTypes = new ArrayList<>();
    private List<String> npkRatios = new ArrayList<>();
    private BigDecimal minPrice = BigDecimal.ZERO;
    private BigDecimal maxPrice = BigDecimal.ZERO;
    private long totalCount = 0;

    public FertilizerFilterOptionsDTO() {
    }

    public List<CategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDTO> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    public List<String> getBrands() {
        return brands;
    }

    public void setBrands(List<String> brands) {
        this.brands = brands != null ? brands : new ArrayList<>();
    }

    public List<String> getFertilizerTypes() {
        return fertilizerTypes;
    }

    public void setFertilizerTypes(List<String> fertilizerTypes) {
        this.fertilizerTypes = fertilizerTypes != null ? fertilizerTypes : new ArrayList<>();
    }

    public List<String> getNpkRatios() {
        return npkRatios;
    }

    public void setNpkRatios(List<String> npkRatios) {
        this.npkRatios = npkRatios != null ? npkRatios : new ArrayList<>();
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
}
