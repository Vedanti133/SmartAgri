package com.smartagri.repository;

import com.smartagri.entity.ReturnRequest;
import com.smartagri.enums.ReturnStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    Optional<ReturnRequest> findByReturnNumber(String returnNumber);

    List<ReturnRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<ReturnRequest> findByOrderId(Long orderId);

    List<ReturnRequest> findByOrderIdAndStatusNotIn(Long orderId, Collection<ReturnStatus> statuses);

    List<ReturnRequest> findByStatusOrderByCreatedAtDesc(ReturnStatus status);

    List<ReturnRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(ReturnStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReturnRequest r WHERE r.id = :id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") Long id);
}
