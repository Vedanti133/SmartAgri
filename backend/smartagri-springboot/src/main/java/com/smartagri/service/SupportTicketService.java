package com.smartagri.service;

import com.smartagri.dto.*;
import com.smartagri.entity.*;
import com.smartagri.enums.MessageSenderType;
import com.smartagri.enums.TicketPriority;
import com.smartagri.enums.TicketStatus;
import com.smartagri.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;

    public SupportTicketService(SupportTicketRepository ticketRepository,
                                SupportTicketMessageRepository messageRepository,
                                UserRepository userRepository,
                                OrderRepository orderRepository,
                                ReturnRequestRepository returnRequestRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.returnRequestRepository = returnRequestRepository;
    }

    public SupportTicketDTO createTicket(Long customerId, CreateSupportTicketRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNumber("TCK-" + System.currentTimeMillis() + "-" + (int) (1000 + Math.random() * 9000));
        ticket.setCustomer(customer);
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);

        if (request.getOrderId() != null) {
            orderRepository.findById(request.getOrderId()).ifPresent(ticket::setOrder);
        }
        if (request.getReturnRequestId() != null) {
            returnRequestRepository.findById(request.getReturnRequestId()).ifPresent(ticket::setReturnRequest);
        }

        ticket = ticketRepository.save(ticket);

        // Add initial message
        SupportTicketMessage initialMsg = new SupportTicketMessage();
        initialMsg.setTicket(ticket);
        initialMsg.setSender(customer);
        initialMsg.setSenderName(customer.getName() != null ? customer.getName() : customer.getUsername());
        initialMsg.setMessage(request.getDescription());
        initialMsg.setSenderType(MessageSenderType.CUSTOMER);
        initialMsg.setCreatedAt(LocalDateTime.now());
        messageRepository.save(initialMsg);

        ticket.getMessages().add(initialMsg);
        return mapToDTO(ticket);
    }

    public SupportTicketDTO addMessage(Long userId, Long ticketId, AddTicketMessageRequest request, boolean isAdmin) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found: " + ticketId));

        if (!isAdmin && !ticket.getCustomer().getId().equals(userId)) {
            throw new SecurityException("Access denied: You cannot view or reply to this ticket");
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        SupportTicketMessage msg = new SupportTicketMessage();
        msg.setTicket(ticket);
        msg.setSender(sender);
        msg.setSenderName(sender.getName() != null ? sender.getName() : sender.getUsername());
        msg.setMessage(request.getMessage());
        msg.setSenderType(isAdmin ? MessageSenderType.ADMIN : MessageSenderType.CUSTOMER);
        msg.setCreatedAt(LocalDateTime.now());
        messageRepository.save(msg);

        // Transition status if appropriate
        if (isAdmin && ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        } else if (!isAdmin && (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.WAITING_FOR_CUSTOMER)) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        ticket.getMessages().add(msg);
        return mapToDTO(ticket);
    }

    public SupportTicketDTO updateStatus(Long ticketId, UpdateTicketStatusRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found: " + ticketId));

        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
            if (request.getStatus() == TicketStatus.RESOLVED) {
                ticket.setResolvedAt(LocalDateTime.now());
            } else if (request.getStatus() == TicketStatus.CLOSED) {
                ticket.setClosedAt(LocalDateTime.now());
            }
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        if (request.getAssignedTo() != null) {
            ticket.setAssignedTo(request.getAssignedTo());
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        return mapToDTO(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public SupportTicketDTO getCustomerTicket(Long customerId, Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found: " + ticketId));

        if (!ticket.getCustomer().getId().equals(customerId)) {
            throw new SecurityException("Access denied: You do not own this ticket");
        }
        return mapToDTO(ticket);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getCustomerTickets(Long customerId) {
        return ticketRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getAllTickets(TicketStatus status) {
        List<SupportTicket> list = (status != null)
                ? ticketRepository.findByStatusOrderByCreatedAtDesc(status)
                : ticketRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupportTicketDTO getTicketById(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found: " + ticketId));
        return mapToDTO(ticket);
    }

    public SupportTicketDTO mapToDTO(SupportTicket ticket) {
        SupportTicketDTO dto = new SupportTicketDTO();
        dto.setId(ticket.getId());
        dto.setTicketNumber(ticket.getTicketNumber());
        if (ticket.getCustomer() != null) {
            dto.setCustomerId(ticket.getCustomer().getId());
            dto.setCustomerName(ticket.getCustomer().getName());
            dto.setCustomerEmail(ticket.getCustomer().getEmail());
        }
        if (ticket.getOrder() != null) {
            dto.setOrderId(ticket.getOrder().getId());
            dto.setOrderTrackingNumber(ticket.getOrder().getOrderNumber());
        }
        if (ticket.getReturnRequest() != null) {
            dto.setReturnRequestId(ticket.getReturnRequest().getId());
            dto.setReturnNumber(ticket.getReturnRequest().getReturnNumber());
        }
        dto.setSubject(ticket.getSubject());
        dto.setDescription(ticket.getDescription());
        dto.setPriority(ticket.getPriority());
        dto.setStatus(ticket.getStatus());
        dto.setAssignedTo(ticket.getAssignedTo());
        dto.setResolvedAt(ticket.getResolvedAt());
        dto.setClosedAt(ticket.getClosedAt());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());

        List<SupportTicketMessage> messages = (ticket.getMessages() != null && !ticket.getMessages().isEmpty())
                ? ticket.getMessages()
                : messageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());

        dto.setMessages(messages.stream().map(m -> {
            SupportTicketMessageDTO mDto = new SupportTicketMessageDTO();
            mDto.setId(m.getId());
            mDto.setTicketId(ticket.getId());
            if (m.getSender() != null) {
                mDto.setSenderId(m.getSender().getId());
            }
            mDto.setSenderName(m.getSenderName());
            mDto.setMessage(m.getMessage());
            mDto.setSenderType(m.getSenderType());
            mDto.setCreatedAt(m.getCreatedAt());
            return mDto;
        }).collect(Collectors.toList()));

        return dto;
    }
}
