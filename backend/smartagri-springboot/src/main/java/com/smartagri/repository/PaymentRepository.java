package com.smartagri.repository;

import com.smartagri.entity.Payment;
import com.smartagri.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByPaymentReference(String paymentReference);
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Payment> findAllByOrderByCreatedAtDesc();
    long countByPaymentStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE " +
           "(:status IS NULL OR p.paymentStatus = :status) AND " +
           "(:method IS NULL OR UPPER(p.paymentMethod) = UPPER(:method)) AND " +
           "(:search IS NULL OR LOWER(p.transactionId) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.order.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR (p.user IS NOT NULL AND LOWER(p.user.name) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "ORDER BY p.createdAt DESC")
    List<Payment> findAdminPaymentsFiltered(
            @Param("status") PaymentStatus status,
            @Param("method") String method,
            @Param("search") String search
    );
}
