package com.smartagri.repository;

import com.smartagri.entity.Refund;
import com.smartagri.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByRefundNumber(String refundNumber);

    Optional<Refund> findByReturnRequestId(Long returnRequestId);

    List<Refund> findByOrderId(Long orderId);

    List<Refund> findByStatus(RefundStatus status);

    List<Refund> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.order.id = :orderId AND r.status IN :statuses")
    BigDecimal sumAmountByOrderIdAndStatusIn(@Param("orderId") Long orderId, @Param("statuses") Collection<RefundStatus> statuses);
}
