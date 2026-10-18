package com.smartagri.repository;

import com.smartagri.entity.ReturnTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnTrackingEventRepository extends JpaRepository<ReturnTrackingEvent, Long> {

    List<ReturnTrackingEvent> findByReturnShipmentIdOrderByEventTimeDesc(Long shipmentId);

    List<ReturnTrackingEvent> findByReturnShipmentIdOrderByEventTimeAsc(Long shipmentId);
}
