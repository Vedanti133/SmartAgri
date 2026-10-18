package com.smartagri.service;

import com.smartagri.dto.CategoryDTO;
import com.smartagri.dto.FertilizerDTO;
import com.smartagri.dto.FertilizerFilterOptionsDTO;
import com.smartagri.dto.PageResponse;
import com.smartagri.entity.Category;
import com.smartagri.entity.Fertilizer;
import com.smartagri.repository.CategoryRepository;
import com.smartagri.repository.FertilizerRepository;
import com.smartagri.repository.FertilizerSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional
public class FertilizerService {

    private final FertilizerRepository fertilizerRepository;
    private final CategoryRepository categoryRepository;

    public FertilizerService(FertilizerRepository fertilizerRepository, CategoryRepository categoryRepository) {
        this.fertilizerRepository = fertilizerRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<FertilizerDTO> getAllFertilizers() {
        return fertilizerRepository.findAll().stream()
                .map(FertilizerDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FertilizerDTO getFertilizerById(Long id) {
        Fertilizer f = fertilizerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with ID: " + id));
        return FertilizerDTO.fromEntity(f);
    }

    @Transactional(readOnly = true)
    public FertilizerDTO getFertilizerBySku(String sku) {
        Fertilizer f = fertilizerRepository.findBySkuIgnoreCase(sku)
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with SKU: " + sku));
        return FertilizerDTO.fromEntity(f);
    }

    public FertilizerDTO createFertilizer(FertilizerDTO dto) {
        validateFertilizerDTO(dto, null);

        Fertilizer fertilizer = new Fertilizer();
        mapDtoToEntity(dto, fertilizer);

        Fertilizer saved = fertilizerRepository.save(fertilizer);
        return FertilizerDTO.fromEntity(saved);
    }

    public FertilizerDTO updateFertilizer(Long id, FertilizerDTO dto) {
        Fertilizer existing = fertilizerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with ID: " + id));

        validateFertilizerDTO(dto, id);
        mapDtoToEntity(dto, existing);

        Fertilizer updated = fertilizerRepository.save(existing);
        return FertilizerDTO.fromEntity(updated);
    }

    public void deleteFertilizer(Long id) {
        Fertilizer existing = fertilizerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with ID: " + id));
        fertilizerRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public List<FertilizerDTO> search(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            return getAllFertilizers();
        }
        String term = keyword.trim();
        return fertilizerRepository
                .findByNameContainingIgnoreCaseOrBrandContainingIgnoreCaseOrSkuContainingIgnoreCase(term, term, term)
                .stream()
                .map(FertilizerDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<FertilizerDTO> searchFertilizers(
            String q,
            Long categoryId,
            String categoryName,
            String brand,
            String fertilizerType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String stockStatus,
            String npk,
            String sort,
            int page,
            int size) {

        Specification<Fertilizer> spec = FertilizerSpecification.build(
                q, categoryId, categoryName, brand, fertilizerType, minPrice, maxPrice, stockStatus, npk
        );

        Sort sortObj = resolveSort(sort);
        int pageNum = Math.max(0, page);
        int pageSize = (size <= 0 || size > 100) ? 12 : size;
        Pageable pageable = PageRequest.of(pageNum, pageSize, sortObj);

        Page<Fertilizer> pageResult = fertilizerRepository.findAll(spec, pageable);

        List<FertilizerDTO> dtos = pageResult.getContent().stream()
                .map(FertilizerDTO::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(
                dtos,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast(),
                pageResult.isEmpty()
        );
    }

    @Transactional(readOnly = true)
    public FertilizerFilterOptionsDTO getFilterOptions() {
        FertilizerFilterOptionsDTO options = new FertilizerFilterOptionsDTO();

        // 1. Categories with product counts
        List<CategoryDTO> categoryDTOs = categoryRepository.findAll().stream()
                .map(cat -> CategoryDTO.fromEntity(cat, fertilizerRepository.countByCategory_Id(cat.getId())))
                .collect(Collectors.toList());
        options.setCategories(categoryDTOs);

        // 2. Distinct Brands
        options.setBrands(fertilizerRepository.findDistinctBrands());

        // 3. Distinct Types
        options.setFertilizerTypes(fertilizerRepository.findDistinctTypes());

        // 4. Distinct NPK Ratios
        options.setNpkRatios(fertilizerRepository.findDistinctNpkRatios());

        // 5. Price Min & Max
        BigDecimal min = fertilizerRepository.findMinSellingPrice();
        BigDecimal max = fertilizerRepository.findMaxSellingPrice();
        options.setMinPrice(min != null ? min : BigDecimal.ZERO);
        options.setMaxPrice(max != null ? max : new BigDecimal("2000.00"));

        // 6. Total active products count
        options.setTotalCount(fertilizerRepository.countByActiveTrue());

        return options;
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.trim().isBlank()) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        String s = sort.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "price_asc", "price-low", "price_low", "price:low_to_high" ->
                    Sort.by(Sort.Direction.ASC, "sellingPrice");
            case "price_desc", "price-high", "price_high", "price:high_to_low" ->
                    Sort.by(Sort.Direction.DESC, "sellingPrice");
            case "name_asc", "name-az", "name:a_to_z", "name-asc" ->
                    Sort.by(Sort.Direction.ASC, "name");
            case "name_desc", "name-za", "name:z_to_a", "name-desc" ->
                    Sort.by(Sort.Direction.DESC, "name");
            case "newest", "recent", "created_desc" ->
                    Sort.by(Sort.Direction.DESC, "createdAt");
            default ->
                    Sort.by(Sort.Direction.ASC, "id");
        };
    }

    @Transactional(readOnly = true)
    public List<FertilizerDTO> getByCategory(Long categoryId) {
        return fertilizerRepository.findByCategory_Id(categoryId).stream()
                .map(FertilizerDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FertilizerDTO> getByBrand(String brand) {
        if (brand == null || brand.trim().isBlank()) {
            return getAllFertilizers();
        }
        return fertilizerRepository.findByBrandIgnoreCase(brand.trim()).stream()
                .map(FertilizerDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private void validateFertilizerDTO(FertilizerDTO dto, Long currentId) {
        if (dto.getName() == null || dto.getName().trim().isBlank()) {
            throw new IllegalArgumentException("Fertilizer product name is required.");
        }

        if (dto.getSku() == null || dto.getSku().trim().isBlank()) {
            throw new IllegalArgumentException("Product SKU is required.");
        }
        String cleanSku = dto.getSku().trim();

        if (currentId == null) {
            if (fertilizerRepository.existsBySkuIgnoreCase(cleanSku)) {
                throw new IllegalArgumentException("Fertilizer with SKU '" + cleanSku + "' already exists.");
            }
        } else {
            if (fertilizerRepository.existsBySkuIgnoreCaseAndIdNot(cleanSku, currentId)) {
                throw new IllegalArgumentException("Another fertilizer with SKU '" + cleanSku + "' already exists.");
            }
        }

        if (dto.getPurchasePrice() != null && dto.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Purchase price cannot be negative.");
        }

        if (dto.getSellingPrice() != null && dto.getSellingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Selling price must be greater than zero.");
        }

        if (dto.getStock() != null && dto.getStock() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }

        if (dto.getMinimumStock() != null && dto.getMinimumStock() < 0) {
            throw new IllegalArgumentException("Minimum stock / reorder level cannot be negative.");
        }

        if (dto.getDiscountPercent() != null) {
            if (dto.getDiscountPercent().compareTo(BigDecimal.ZERO) < 0 ||
                    dto.getDiscountPercent().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
            }
        }

        if (dto.getTaxPercent() != null) {
            if (dto.getTaxPercent().compareTo(BigDecimal.ZERO) < 0 ||
                    dto.getTaxPercent().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Tax percentage must be between 0 and 100.");
            }
        }
    }

    private void mapDtoToEntity(FertilizerDTO dto, Fertilizer entity) {
        entity.setName(dto.getName().trim());
        entity.setSku(dto.getSku().trim());
        entity.setBrand(dto.getBrand() != null ? dto.getBrand().trim() : null);

        // Resolve Category
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + dto.getCategoryId()));
        } else if (dto.getCategoryName() != null && !dto.getCategoryName().trim().isBlank()) {
            String catName = dto.getCategoryName().trim();
            category = categoryRepository.findByNameIgnoreCase(catName)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with name: " + catName));
        } else if (dto.getCategory() != null && !dto.getCategory().trim().isBlank()) {
            String catName = dto.getCategory().trim();
            category = categoryRepository.findByNameIgnoreCase(catName)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with name: " + catName));
        }

        if (category != null) {
            entity.setCategory(category);
        }

        entity.setUnit(dto.getUnit() != null ? dto.getUnit().trim() : "Bag");
        entity.setPurchasePrice(dto.getPurchasePrice() != null ? dto.getPurchasePrice() : BigDecimal.ZERO);
        entity.setSellingPrice(dto.getSellingPrice() != null ? dto.getSellingPrice() : BigDecimal.ZERO);
        entity.setStock(dto.getStock() != null ? dto.getStock() : 0);
        entity.setMinimumStock(dto.getMinimumStock() != null ? dto.getMinimumStock() : 10);
        entity.setImageUrl(dto.getImageUrl());
        entity.setSuitableCrops(dto.getSuitableCrops());
        entity.setSuitableSoil(dto.getSuitableSoil());
        entity.setComposition(dto.getComposition());
        entity.setUsageInstructions(dto.getUsageInstructions());
        entity.setDiscountPercent(dto.getDiscountPercent() != null ? dto.getDiscountPercent() : BigDecimal.ZERO);
        entity.setTaxPercent(dto.getTaxPercent() != null ? dto.getTaxPercent() : new BigDecimal("5.00"));
        entity.setWeightValue(dto.getWeightValue());

        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            entity.setStatus(dto.getStatus().trim());
        }

        entity.setType(dto.getType());
        entity.setNpkRatio(dto.getNpkRatio());
        entity.setSupplier(dto.getSupplier());
        entity.setExpiryDate(dto.getExpiryDate());
        entity.setDescription(dto.getDescription());

        entity.syncStatusAndStock();
    }
}
