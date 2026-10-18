package com.smartagri.repository;

import com.smartagri.entity.Shipment;
import com.smartagri.enums.ShipmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    List<Shipment> findByOrderId(Long orderId);

    Optional<Shipment> findByShipmentNumber(String shipmentNumber);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    List<Shipment> findByWarehouseId(Long warehouseId);

    List<Shipment> findByStatus(ShipmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Shipment s WHERE s.id = :id")
    Optional<Shipment> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT s FROM Shipment s WHERE s.order.id = :orderId ORDER BY s.createdAt DESC")
    List<Shipment> findLatestByOrderId(@Param("orderId") Long orderId);
}
