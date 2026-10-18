package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.FertilizerDTO;
import com.smartagri.service.FertilizerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fertilizers")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class FertilizerController {

    private final FertilizerService fertilizerService;

    public FertilizerController(FertilizerService fertilizerService) {
        this.fertilizerService = fertilizerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FertilizerDTO>>> all() {
        return ResponseEntity.ok(ApiResponse.ok(fertilizerService.getAllFertilizers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FertilizerDTO>> one(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(fertilizerService.getFertilizerById(id)));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<FertilizerDTO>> bySku(@PathVariable String sku) {
        return ResponseEntity.ok(ApiResponse.ok(fertilizerService.getFertilizerBySku(sku)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String fertilizerType,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) String stock,
            @RequestParam(required = false) String npk,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size) {

        // Legacy compatibility: If keyword is passed alone without q or filters, return List<FertilizerDTO>
        if (keyword != null && (q == null || q.isBlank()) && categoryId == null && category == null
                && brand == null && fertilizerType == null && type == null && minPrice == null
                && maxPrice == null && stockStatus == null && stock == null && npk == null && sort == null
                && page == 0 && size == 12) {
            return ResponseEntity.ok(ApiResponse.ok(fertilizerService.search(keyword)));
        }

        String searchTerm = (q != null) ? q : keyword;
        String catName = (category != null && !category.isBlank()) ? category : null;
        String fType = (fertilizerType != null && !fertilizerType.isBlank()) ? fertilizerType : type;
        String sStatus = (stockStatus != null && !stockStatus.isBlank()) ? stockStatus : stock;

        com.smartagri.dto.PageResponse<FertilizerDTO> result = fertilizerService.searchFertilizers(
                searchTerm, categoryId, catName, brand, fType, minPrice, maxPrice, sStatus, npk, sort, page, size
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/filter-options")
    public ResponseEntity<ApiResponse<com.smartagri.dto.FertilizerFilterOptionsDTO>> filterOptions() {
        return ResponseEntity.ok(ApiResponse.ok(fertilizerService.getFilterOptions()));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<FertilizerDTO>>> byCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.ok(fertilizerService.getByCategory(categoryId)));
    }

    @GetMapping("/brand/{brand}")
    public ResponseEntity<ApiResponse<List<FertilizerDTO>>> byBrand(@PathVariable String brand) {
        return ResponseEntity.ok(ApiResponse.ok(fertilizerService.getByBrand(brand)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FertilizerDTO>> add(@RequestBody FertilizerDTO dto) {
        FertilizerDTO created = fertilizerService.createFertilizer(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Fertilizer created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FertilizerDTO>> update(@PathVariable Long id, @RequestBody FertilizerDTO dto) {
        FertilizerDTO updated = fertilizerService.updateFertilizer(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Fertilizer updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        fertilizerService.deleteFertilizer(id);
        return ResponseEntity.ok(ApiResponse.ok("Fertilizer deleted successfully", null));
    }
}
