package com.smartagri.repository;

import com.smartagri.entity.Warehouse;
import com.smartagri.entity.WarehouseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByWarehouseCode(String warehouseCode);

    boolean existsByWarehouseCode(String warehouseCode);

    Optional<Warehouse> findFirstByIsDefaultReceivingTrueAndStatus(WarehouseStatus status);

    default Optional<Warehouse> findDefaultReceivingWarehouse() {
        return findFirstByIsDefaultReceivingTrueAndStatus(WarehouseStatus.ACTIVE);
    }

    List<Warehouse> findByStatus(WarehouseStatus status);

    default List<Warehouse> findActiveWarehouses() {
        return findByStatus(WarehouseStatus.ACTIVE);
    }

    List<Warehouse> findByWarehouseNameContainingIgnoreCaseOrWarehouseCodeContainingIgnoreCase(String name, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Warehouse w WHERE w.id = :id")
    Optional<Warehouse> findByIdWithLock(@Param("id") Long id);
}
