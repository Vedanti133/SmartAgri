package com.smartagri.repository;

import com.smartagri.entity.BatchLocationStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchLocationStockRepository extends JpaRepository<BatchLocationStock, Long> {

    Optional<BatchLocationStock> findByBatchIdAndLocationId(Long batchId, Long locationId);

    @Query("SELECT b FROM BatchLocationStock b WHERE b.batch.id = :batchId AND b.warehouse.id = :warehouseId AND b.location.id = :locationId")
    Optional<BatchLocationStock> findByBatchIdAndWarehouseIdAndLocationId(@Param("batchId") Long batchId, @Param("warehouseId") Long warehouseId, @Param("locationId") Long locationId);

    List<BatchLocationStock> findByBatchId(Long batchId);

    List<BatchLocationStock> findByWarehouseId(Long warehouseId);

    List<BatchLocationStock> findByLocationId(Long locationId);

    List<BatchLocationStock> findByBatchFertilizerId(Long fertilizerId);

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM BatchLocationStock b WHERE b.batch.id = :batchId")
    Integer sumQuantityByBatchId(@Param("batchId") Long batchId);

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM BatchLocationStock b WHERE b.location.id = :locationId")
    Integer sumQuantityByLocationId(@Param("locationId") Long locationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BatchLocationStock b WHERE b.id = :id")
    Optional<BatchLocationStock> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BatchLocationStock b WHERE b.batch.id = :batchId AND b.location.id = :locationId")
    Optional<BatchLocationStock> findByBatchIdAndLocationIdWithLock(@Param("batchId") Long batchId, @Param("locationId") Long locationId);
}
