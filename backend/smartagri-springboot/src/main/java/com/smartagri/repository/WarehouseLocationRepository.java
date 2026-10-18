package com.smartagri.repository;

import com.smartagri.entity.LocationStatus;
import com.smartagri.entity.WarehouseLocation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {

    List<WarehouseLocation> findByWarehouseId(Long warehouseId);

    Optional<WarehouseLocation> findByWarehouseIdAndLocationCode(Long warehouseId, String locationCode);

    boolean existsByWarehouseIdAndLocationCode(Long warehouseId, String locationCode);

    List<WarehouseLocation> findByParentLocationId(Long parentId);

    List<WarehouseLocation> findByWarehouseIdAndStatus(Long warehouseId, LocationStatus status);

    default List<WarehouseLocation> findActiveLocationsByWarehouseId(Long warehouseId) {
        return findByWarehouseIdAndStatus(warehouseId, LocationStatus.ACTIVE);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM WarehouseLocation l WHERE l.id = :id")
    Optional<WarehouseLocation> findByIdWithLock(@Param("id") Long id);
}
