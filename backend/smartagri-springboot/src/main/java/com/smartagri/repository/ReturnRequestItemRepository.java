package com.smartagri.repository;

import com.smartagri.entity.ReturnRequestItem;
import com.smartagri.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReturnRequestItemRepository extends JpaRepository<ReturnRequestItem, Long> {

    List<ReturnRequestItem> findByReturnRequestId(Long returnRequestId);

    List<ReturnRequestItem> findByOrderItemId(Long orderItemId);

    @Query("SELECT COALESCE(SUM(ri.requestedQuantity), 0) FROM ReturnRequestItem ri WHERE ri.orderItem.id = :orderItemId AND ri.returnRequest.status NOT IN :excludedStatuses")
    Integer sumRequestedQuantityByOrderItemIdAndStatusNotIn(@Param("orderItemId") Long orderItemId, @Param("excludedStatuses") Collection<ReturnStatus> excludedStatuses);
}
