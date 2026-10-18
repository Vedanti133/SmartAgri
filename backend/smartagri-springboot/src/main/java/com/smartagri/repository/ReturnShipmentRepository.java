package com.smartagri.repository;

import com.smartagri.entity.ReturnShipment;
import com.smartagri.enums.ReturnShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnShipmentRepository extends JpaRepository<ReturnShipment, Long> {

    Optional<ReturnShipment> findByReturnRequestId(Long returnRequestId);

    Optional<ReturnShipment> findByShipmentNumber(String shipmentNumber);

    Optional<ReturnShipment> findByTrackingNumber(String trackingNumber);

    List<ReturnShipment> findByStatus(ReturnShipmentStatus status);
}
