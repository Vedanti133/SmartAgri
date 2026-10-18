package com.smartagri.repository;

import com.smartagri.entity.SupportTicket;
import com.smartagri.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    List<SupportTicket> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<SupportTicket> findByOrderId(Long orderId);

    List<SupportTicket> findByReturnRequestId(Long returnRequestId);

    List<SupportTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status);

    List<SupportTicket> findAllByOrderByCreatedAtDesc();

    long countByStatus(TicketStatus status);
}
