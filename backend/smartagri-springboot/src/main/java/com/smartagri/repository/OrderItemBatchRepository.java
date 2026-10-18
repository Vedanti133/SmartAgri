package com.smartagri.repository;

import com.smartagri.entity.OrderItemBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemBatchRepository extends JpaRepository<OrderItemBatch, Long> {

    List<OrderItemBatch> findByOrderItemId(Long orderItemId);

    List<OrderItemBatch> findByBatchId(Long batchId);

    @Query("SELECT oib FROM OrderItemBatch oib WHERE oib.orderItem.order.id = :orderId")
    List<OrderItemBatch> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT oib FROM OrderItemBatch oib WHERE oib.orderItem.order.orderNumber = :orderNumber AND oib.orderItem.fertilizer.id = :fertilizerId")
    List<OrderItemBatch> findByOrderNumberAndFertilizerId(@Param("orderNumber") String orderNumber,
                                                         @Param("fertilizerId") Long fertilizerId);

    boolean existsByOrderItemId(Long orderItemId);
}
