package com.smartagri.repository;

import com.smartagri.entity.Fertilizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FertilizerRepository extends JpaRepository<Fertilizer, Long>, JpaSpecificationExecutor<Fertilizer> {

    Optional<Fertilizer> findBySku(String sku);

    Optional<Fertilizer> findBySkuIgnoreCase(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    List<Fertilizer> findByCategory_Id(Long categoryId);

    List<Fertilizer> findByCategory_NameIgnoreCase(String categoryName);

    List<Fertilizer> findByBrandIgnoreCase(String brand);

    List<Fertilizer> findByStatusIgnoreCase(String status);

    long countByCategory_Id(Long categoryId);

    long countByActiveTrue();

    List<Fertilizer> findByNameContainingIgnoreCaseOrBrandContainingIgnoreCaseOrSkuContainingIgnoreCase(
            String name, String brand, String sku);

    @Query("SELECT DISTINCT f.brand FROM Fertilizer f WHERE f.brand IS NOT NULL AND f.brand != '' AND f.active = true ORDER BY f.brand")
    List<String> findDistinctBrands();

    @Query("SELECT DISTINCT f.type FROM Fertilizer f WHERE f.type IS NOT NULL AND f.type != '' AND f.active = true ORDER BY f.type")
    List<String> findDistinctTypes();

    @Query("SELECT DISTINCT f.npkRatio FROM Fertilizer f WHERE f.npkRatio IS NOT NULL AND f.npkRatio != '' AND f.active = true ORDER BY f.npkRatio")
    List<String> findDistinctNpkRatios();

    @Query("SELECT MIN(f.sellingPrice) FROM Fertilizer f WHERE f.active = true")
    BigDecimal findMinSellingPrice();

    @Query("SELECT MAX(f.sellingPrice) FROM Fertilizer f WHERE f.active = true")
    BigDecimal findMaxSellingPrice();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Fertilizer f WHERE f.id = :id")
    Optional<Fertilizer> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Long id);

    @Query("SELECT f FROM Fertilizer f WHERE (f.stock - f.reservedStock) <= f.lowStockThreshold AND (f.stock - f.reservedStock) > 0 AND f.active = true ORDER BY (f.stock - f.reservedStock) ASC")
    List<Fertilizer> findLowStockFertilizers();

    @Query("SELECT f FROM Fertilizer f WHERE (f.stock - f.reservedStock) <= 0 AND f.active = true ORDER BY f.name ASC")
    List<Fertilizer> findOutOfStockFertilizers();

    @Query("SELECT COALESCE(SUM(f.stock), 0) FROM Fertilizer f")
    Long countTotalStockUnits();

    @Query("SELECT COUNT(f) FROM Fertilizer f WHERE (f.stock - f.reservedStock) <= f.lowStockThreshold AND (f.stock - f.reservedStock) > 0 AND f.active = true")
    Long countLowStockProducts();

    @Query("SELECT COUNT(f) FROM Fertilizer f WHERE (f.stock - f.reservedStock) <= 0 AND f.active = true")
    Long countOutOfStockProducts();

    @Query("SELECT COUNT(f) FROM Fertilizer f WHERE f.stock < 0")
    Long countNegativeStockProducts();
}
