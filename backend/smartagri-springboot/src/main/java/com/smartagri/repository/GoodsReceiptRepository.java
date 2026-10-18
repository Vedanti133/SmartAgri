package com.smartagri.repository;

import com.smartagri.entity.GoodsReceipt;
import com.smartagri.entity.GoodsReceiptStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT grn FROM GoodsReceipt grn WHERE grn.id = :id")
    Optional<GoodsReceipt> findByIdWithLock(@Param("id") Long id);

    Optional<GoodsReceipt> findByGrnNumber(String grnNumber);

    boolean existsByGrnNumber(String grnNumber);

    List<GoodsReceipt> findByPurchaseOrderIdOrderByCreatedAtDesc(Long purchaseOrderId);

    List<GoodsReceipt> findByStatusOrderByCreatedAtDesc(GoodsReceiptStatus status);

    @Query("SELECT grn FROM GoodsReceipt grn WHERE " +
           "(:poId IS NULL OR grn.purchaseOrder.id = :poId) AND " +
           "(:status IS NULL OR grn.status = :status) AND " +
           "(:search IS NULL OR LOWER(grn.grnNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(grn.purchaseOrder.poNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(grn.supplierInvoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY grn.createdAt DESC")
    List<GoodsReceipt> searchGoodsReceipts(@Param("poId") Long poId,
                                          @Param("status") GoodsReceiptStatus status,
                                          @Param("search") String search);

    long countByStatus(GoodsReceiptStatus status);
}
