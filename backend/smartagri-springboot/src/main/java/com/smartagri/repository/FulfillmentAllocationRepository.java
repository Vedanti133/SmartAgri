package com.smartagri.repository;

import com.smartagri.entity.FulfillmentAllocation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FulfillmentAllocationRepository extends JpaRepository<FulfillmentAllocation, Long> {

    List<FulfillmentAllocation> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fa FROM FulfillmentAllocation fa WHERE fa.order.id = :orderId")
    List<FulfillmentAllocation> findByOrderIdWithLock(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fa FROM FulfillmentAllocation fa WHERE fa.id = :id")
    Optional<FulfillmentAllocation> findByIdWithLock(@Param("id") Long id);

    Optional<FulfillmentAllocation> findByAllocationNumber(String allocationNumber);

    List<FulfillmentAllocation> findByWarehouseId(Long warehouseId);

    List<FulfillmentAllocation> findByStatus(String status);
}
