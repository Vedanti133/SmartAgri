package com.smartagri.repository;

import com.smartagri.entity.InventoryTransaction;
import com.smartagri.entity.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByFertilizerIdOrderByCreatedAtDesc(Long fertilizerId);

    List<InventoryTransaction> findAllByOrderByCreatedAtDesc();

    boolean existsByTransactionTypeAndReferenceIdAndFertilizerId(
            InventoryTransactionType transactionType, String referenceId, Long fertilizerId);

    boolean existsByTransactionTypeAndReferenceId(
            InventoryTransactionType transactionType, String referenceId);

    boolean existsByReferenceTypeAndReferenceIdAndFertilizerId(
            String referenceType, String referenceId, Long fertilizerId);

    @Query("SELECT t FROM InventoryTransaction t WHERE " +
           "(:fertilizerId IS NULL OR t.fertilizer.id = :fertilizerId) AND " +
           "(:type IS NULL OR t.transactionType = :type) AND " +
           "(:search IS NULL OR LOWER(t.fertilizer.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.fertilizer.sku) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.referenceId) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.performedBy) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.createdAt DESC")
    List<InventoryTransaction> findFiltered(
            @Param("fertilizerId") Long fertilizerId,
            @Param("type") InventoryTransactionType type,
            @Param("search") String search);

    @Query("SELECT t FROM InventoryTransaction t WHERE " +
           "(:fertilizerId IS NULL OR t.fertilizer.id = :fertilizerId) AND " +
           "(:type IS NULL OR t.transactionType = :type) AND " +
           "(:search IS NULL OR LOWER(t.fertilizer.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.fertilizer.sku) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.referenceId) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.performedBy) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<InventoryTransaction> findFilteredPaged(
            @Param("fertilizerId") Long fertilizerId,
            @Param("type") InventoryTransactionType type,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM InventoryTransaction t WHERE t.transactionType IN :types AND t.createdAt >= :since")
    Long sumQuantityByTypesSince(@Param("types") Collection<InventoryTransactionType> types, @Param("since") LocalDateTime since);

    long countByFertilizerId(Long fertilizerId);
}
