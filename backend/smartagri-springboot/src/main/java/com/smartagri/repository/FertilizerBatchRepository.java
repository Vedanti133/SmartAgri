package com.smartagri.repository;

import com.smartagri.entity.BatchStatus;
import com.smartagri.entity.FertilizerBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FertilizerBatchRepository extends JpaRepository<FertilizerBatch, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM FertilizerBatch b WHERE b.id = :id")
    Optional<FertilizerBatch> findByIdWithLock(@Param("id") Long id);

    List<FertilizerBatch> findByFertilizerIdOrderByExpiryDateAsc(Long fertilizerId);

    List<FertilizerBatch> findByFertilizerId(Long fertilizerId);

    Optional<FertilizerBatch> findByFertilizerIdAndBatchNumber(Long fertilizerId, String batchNumber);

    boolean existsByFertilizerIdAndBatchNumber(Long fertilizerId, String batchNumber);

    List<FertilizerBatch> findByStatus(BatchStatus status);

    // FEFO query with Pessimistic Write Lock:
    // Available quantity > 0, expiryDate >= currentDate, status not BLOCKED, ordered by earliest expiry date first
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM FertilizerBatch b WHERE b.fertilizer.id = :fertilizerId " +
           "AND (b.currentQuantity - b.reservedQuantity) > 0 " +
           "AND b.expiryDate >= :currentDate " +
           "AND b.status <> com.smartagri.entity.BatchStatus.BLOCKED " +
           "ORDER BY b.expiryDate ASC, b.id ASC")
    List<FertilizerBatch> findSaleableBatchesFEFOWithLock(@Param("fertilizerId") Long fertilizerId,
                                                          @Param("currentDate") LocalDate currentDate);

    // FEFO query without lock for read-only availability calculation
    @Query("SELECT b FROM FertilizerBatch b WHERE b.fertilizer.id = :fertilizerId " +
           "AND (b.currentQuantity - b.reservedQuantity) > 0 " +
           "AND b.expiryDate >= :currentDate " +
           "AND b.status <> com.smartagri.entity.BatchStatus.BLOCKED " +
           "ORDER BY b.expiryDate ASC, b.id ASC")
    List<FertilizerBatch> findSaleableBatchesFEFO(@Param("fertilizerId") Long fertilizerId,
                                                 @Param("currentDate") LocalDate currentDate);

    // Near expiry batches: expiryDate between today and warningThresholdDate, not blocked, currentQuantity > 0
    @Query("SELECT b FROM FertilizerBatch b WHERE b.expiryDate >= :today AND b.expiryDate <= :warningThreshold " +
           "AND b.status <> com.smartagri.entity.BatchStatus.BLOCKED AND b.currentQuantity > 0 " +
           "ORDER BY b.expiryDate ASC")
    List<FertilizerBatch> findNearExpiryBatches(@Param("today") LocalDate today,
                                               @Param("warningThreshold") LocalDate warningThreshold);

    // Expired batches: expiryDate < today
    @Query("SELECT b FROM FertilizerBatch b WHERE b.expiryDate < :today ORDER BY b.expiryDate DESC")
    List<FertilizerBatch> findExpiredBatches(@Param("today") LocalDate today);

    // Sum of available units across all batches
    @Query("SELECT COALESCE(SUM(b.currentQuantity), 0) FROM FertilizerBatch b")
    Long sumTotalBatchUnits();

    // Sum of available units for near-expiry batches
    @Query("SELECT COALESCE(SUM(b.currentQuantity), 0) FROM FertilizerBatch b " +
           "WHERE b.expiryDate >= :today AND b.expiryDate <= :warningThreshold " +
           "AND b.status <> com.smartagri.entity.BatchStatus.BLOCKED AND b.currentQuantity > 0")
    Long sumNearExpiryUnits(@Param("today") LocalDate today, @Param("warningThreshold") LocalDate warningThreshold);

    // Sum of units for expired batches
    @Query("SELECT COALESCE(SUM(b.currentQuantity), 0) FROM FertilizerBatch b WHERE b.expiryDate < :today")
    Long sumExpiredUnits(@Param("today") LocalDate today);

    // Check count of batches for a fertilizer
    long countByFertilizerId(Long fertilizerId);
}
