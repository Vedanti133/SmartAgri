package com.smartagri.repository;

import com.smartagri.entity.Delivery;
import com.smartagri.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByOrderId(Long orderId);
    Optional<Delivery> findByTrackingNumber(String trackingNumber);
    List<Delivery> findByDeliveryStatusOrderByCreatedAtDesc(DeliveryStatus status);
    List<Delivery> findAllByOrderByCreatedAtDesc();
    long countByDeliveryStatus(DeliveryStatus status);
    long countByDeliveredAtAfter(LocalDateTime dateTime);
}
