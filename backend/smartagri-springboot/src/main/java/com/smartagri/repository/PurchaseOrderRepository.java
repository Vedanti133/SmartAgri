package com.smartagri.repository;

import com.smartagri.entity.PurchaseOrder;
import com.smartagri.entity.PurchaseOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM PurchaseOrder po WHERE po.id = :id")
    Optional<PurchaseOrder> findByIdWithLock(@Param("id") Long id);

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    boolean existsByPoNumber(String poNumber);

    List<PurchaseOrder> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(PurchaseOrderStatus status);

    @Query("SELECT po FROM PurchaseOrder po WHERE " +
           "(:supplierId IS NULL OR po.supplier.id = :supplierId) AND " +
           "(:status IS NULL OR po.status = :status) AND " +
           "(:search IS NULL OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(po.supplier.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY po.createdAt DESC")
    List<PurchaseOrder> searchPurchaseOrders(@Param("supplierId") Long supplierId,
                                            @Param("status") PurchaseOrderStatus status,
                                            @Param("search") String search);

    long countByStatus(PurchaseOrderStatus status);

    @Query("SELECT COALESCE(SUM(po.totalAmount), 0) FROM PurchaseOrder po WHERE po.createdAt >= :since AND po.status NOT IN (com.smartagri.entity.PurchaseOrderStatus.CANCELLED, com.smartagri.entity.PurchaseOrderStatus.DRAFT)")
    BigDecimal calculatePurchaseValueSince(@Param("since") LocalDateTime since);
}
