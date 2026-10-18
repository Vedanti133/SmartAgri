package com.smartagri.repository;

import com.smartagri.entity.ShipmentTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentTrackingEventRepository extends JpaRepository<ShipmentTrackingEvent, Long> {
    List<ShipmentTrackingEvent> findByShipmentIdOrderByEventTimeAsc(Long shipmentId);
}
