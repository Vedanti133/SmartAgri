package com.smartagri.service;

import com.smartagri.dto.CategoryDTO;
import com.smartagri.entity.Category;
import com.smartagri.repository.CategoryRepository;
import com.smartagri.repository.FertilizerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final FertilizerRepository fertilizerRepository;

    public CategoryService(CategoryRepository categoryRepository, FertilizerRepository fertilizerRepository) {
        this.categoryRepository = categoryRepository;
        this.fertilizerRepository = fertilizerRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(cat -> {
                    long count = fertilizerRepository.countByCategory_Id(cat.getId());
                    return CategoryDTO.fromEntity(cat, count);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found with ID: " + id));
        long count = fertilizerRepository.countByCategory_Id(cat.getId());
        return CategoryDTO.fromEntity(cat, count);
    }

    public CategoryDTO createCategory(CategoryDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isBlank()) {
            throw new IllegalArgumentException("Category name is required.");
        }
        String cleanName = dto.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(cleanName)) {
            throw new IllegalArgumentException("Category with name '" + cleanName + "' already exists.");
        }

        Category category = new Category();
        category.setName(cleanName);
        category.setDescription(dto.getDescription());
        category.setStatus(dto.getStatus() != null && !dto.getStatus().isBlank() ? dto.getStatus().trim() : "ACTIVE");

        Category saved = categoryRepository.save(category);
        return CategoryDTO.fromEntity(saved, 0L);
    }

    public CategoryDTO updateCategory(Long id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found with ID: " + id));

        if (dto.getName() == null || dto.getName().trim().isBlank()) {
            throw new IllegalArgumentException("Category name is required.");
        }
        String cleanName = dto.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(cleanName, id)) {
            throw new IllegalArgumentException("Another category with name '" + cleanName + "' already exists.");
        }

        category.setName(cleanName);
        category.setDescription(dto.getDescription());
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            category.setStatus(dto.getStatus().trim());
        }

        Category updated = categoryRepository.save(category);
        long count = fertilizerRepository.countByCategory_Id(updated.getId());
        return CategoryDTO.fromEntity(updated, count);
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found with ID: " + id));

        long count = fertilizerRepository.countByCategory_Id(id);
        if (count > 0) {
            throw new IllegalArgumentException("Cannot delete category '" + category.getName() +
                    "' because " + count + " fertilizer product(s) are associated with it. Please reassign or delete them first.");
        }

        categoryRepository.delete(category);
    }
}
