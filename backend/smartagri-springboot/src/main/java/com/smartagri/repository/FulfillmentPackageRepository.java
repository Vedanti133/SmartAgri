package com.smartagri.repository;

import com.smartagri.entity.FulfillmentPackage;
import com.smartagri.enums.PackageStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FulfillmentPackageRepository extends JpaRepository<FulfillmentPackage, Long> {

    List<FulfillmentPackage> findByOrderId(Long orderId);

    Optional<FulfillmentPackage> findByPackageNumber(String packageNumber);

    List<FulfillmentPackage> findByWarehouseId(Long warehouseId);

    List<FulfillmentPackage> findByPackageStatus(PackageStatus packageStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fp FROM FulfillmentPackage fp WHERE fp.id = :id")
    Optional<FulfillmentPackage> findByIdWithLock(@Param("id") Long id);
}
