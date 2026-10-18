package com.smartagri.repository;

import com.smartagri.entity.ReplacementOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReplacementOrderRepository extends JpaRepository<ReplacementOrder, Long> {

    Optional<ReplacementOrder> findByReturnRequestId(Long returnRequestId);

    Optional<ReplacementOrder> findByReplacementOrderId(Long replacementOrderId);

    List<ReplacementOrder> findByOriginalOrderId(Long originalOrderId);
}
