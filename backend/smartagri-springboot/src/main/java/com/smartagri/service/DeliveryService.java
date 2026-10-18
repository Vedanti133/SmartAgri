package com.smartagri.service;

import com.smartagri.dto.AssignDeliveryRequest;
import com.smartagri.dto.DeliveryDTO;
import com.smartagri.entity.*;
import com.smartagri.repository.DeliveryRepository;
import com.smartagri.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           OrderRepository orderRepository,
                           @Lazy PaymentService paymentService) {
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    @Transactional
    public Delivery createInitialDelivery(Order order) {
        Optional<Delivery> existing = deliveryRepository.findByOrderId(order.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setDeliveryStatus(DeliveryStatus.NOT_ASSIGNED);
        delivery.setDeliveryNotes("Order created, waiting for fulfillment assignment.");

        Delivery saved = deliveryRepository.save(delivery);
        log.info("Created initial delivery #{} for order #{}", saved.getId(), order.getOrderNumber());
        return saved;
    }

    @Transactional(readOnly = true)
    public DeliveryDTO getDeliveryForOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this order.");
        }

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseGet(() -> createInitialDelivery(order));

        return toDTO(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryDTO getDeliveryAdmin(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found with id: " + deliveryId));
        return toDTO(delivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryDTO> getAllDeliveriesAdmin(String statusFilter, String search) {
        List<Delivery> list;
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter.trim())) {
            try {
                DeliveryStatus status = DeliveryStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT));
                list = deliveryRepository.findByDeliveryStatusOrderByCreatedAtDesc(status);
            } catch (IllegalArgumentException e) {
                list = deliveryRepository.findAllByOrderByCreatedAtDesc();
            }
        } else {
            list = deliveryRepository.findAllByOrderByCreatedAtDesc();
        }

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase(Locale.ROOT);
            list = list.stream().filter(d -> {
                String ordNum = d.getOrder() != null ? d.getOrder().getOrderNumber().toLowerCase(Locale.ROOT) : "";
                String trk = d.getTrackingNumber() != null ? d.getTrackingNumber().toLowerCase(Locale.ROOT) : "";
                String partner = d.getDeliveryPartner() != null ? d.getDeliveryPartner().toLowerCase(Locale.ROOT) : "";
                String recipient = d.getOrder() != null ? d.getOrder().getRecipientName().toLowerCase(Locale.ROOT) : "";
                return ordNum.contains(q) || trk.contains(q) || partner.contains(q) || recipient.contains(q);
            }).collect(Collectors.toList());
        }

        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public DeliveryDTO assignDeliveryPartner(Long deliveryId, AssignDeliveryRequest request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found with id: " + deliveryId));

        delivery.setDeliveryPartner(request.getDeliveryPartner().trim());
        delivery.setDeliveryPartnerPhone(request.getDeliveryPartnerPhone().trim());

        if (request.getEstimatedDeliveryDate() != null) {
            delivery.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        } else if (delivery.getEstimatedDeliveryDate() == null) {
            // Default 3 days from today if not specified
            delivery.setEstimatedDeliveryDate(LocalDate.now().plusDays(3));
        }

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            delivery.setDeliveryNotes(request.getNotes().trim());
        }

        if (delivery.getDeliveryStatus() == DeliveryStatus.NOT_ASSIGNED) {
            delivery.setDeliveryStatus(DeliveryStatus.ASSIGNED);
        }

        // Generate tracking number if not yet generated
        if (delivery.getTrackingNumber() == null) {
            delivery.setTrackingNumber(generateTrackingNumber());
        }

        Delivery saved = deliveryRepository.save(delivery);
        log.info("Assigned partner {} to delivery #{}", saved.getDeliveryPartner(), saved.getId());
        return toDTO(saved);
    }

    @Transactional
    public DeliveryDTO updateDeliveryStatus(Long deliveryId, String newStatusStr, String notes) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found with id: " + deliveryId));

        DeliveryStatus newStatus;
        try {
            newStatus = DeliveryStatus.valueOf(newStatusStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid delivery status: " + newStatusStr);
        }

        if (!delivery.getDeliveryStatus().canTransitionTo(newStatus)) {
            throw new IllegalArgumentException("Invalid delivery transition from " +
                    delivery.getDeliveryStatus() + " to " + newStatus);
        }

        delivery.setDeliveryStatus(newStatus);
        if (notes != null && !notes.isBlank()) {
            delivery.setDeliveryNotes(notes.trim());
        }

        LocalDateTime now = LocalDateTime.now();

        // Ensure tracking number exists if moving past PACKED
        if ((newStatus == DeliveryStatus.SHIPPED || newStatus == DeliveryStatus.OUT_FOR_DELIVERY || newStatus == DeliveryStatus.DELIVERED)
                && delivery.getTrackingNumber() == null) {
            delivery.setTrackingNumber(generateTrackingNumber());
        }

        Order order = delivery.getOrder();

        switch (newStatus) {
            case PACKED -> {
                if (order != null && order.getOrderStatus() == OrderStatus.PROCESSING) {
                    order.setOrderStatus(OrderStatus.PACKED);
                    orderRepository.save(order);
                }
            }
            case SHIPPED -> {
                if (delivery.getShippedAt() == null) {
                    delivery.setShippedAt(now);
                }
                if (order != null && (order.getOrderStatus() == OrderStatus.PROCESSING || order.getOrderStatus() == OrderStatus.PACKED)) {
                    order.setOrderStatus(OrderStatus.SHIPPED);
                    orderRepository.save(order);
                }
            }
            case OUT_FOR_DELIVERY -> {
                if (delivery.getOutForDeliveryAt() == null) {
                    delivery.setOutForDeliveryAt(now);
                }
            }
            case DELIVERED -> {
                if (delivery.getDeliveredAt() == null) {
                    delivery.setDeliveredAt(now);
                }
                if (order != null) {
                    order.setOrderStatus(OrderStatus.DELIVERED);
                    if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                        order.setPaymentStatus(PaymentStatus.PAID);
                    }
                    orderRepository.save(order);
                    // Also sync payment record
                    if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                        paymentService.updatePaymentStatusAdmin(order.getId(),
                                new com.smartagri.dto.UpdatePaymentStatusRequest("PAID", null, "Paid on delivery"));
                    }
                }
            }
            case FAILED, RETURNED -> {
                log.warn("Delivery #{} marked as {}", delivery.getId(), newStatus);
            }
            default -> {}
        }

        Delivery saved = deliveryRepository.save(delivery);
        log.info("Updated delivery #{} status to {}", saved.getId(), newStatus);
        return toDTO(saved);
    }

    public String generateTrackingNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        String candidate;
        int attempts = 0;
        do {
            int randomSeq = ThreadLocalRandom.current().nextInt(100000, 999999);
            candidate = String.format(Locale.ROOT, "SA-TRK-%s-%06d", datePart, randomSeq);
            attempts++;
            if (attempts > 50) {
                candidate = String.format(Locale.ROOT, "SA-TRK-%s-%06d", datePart, System.currentTimeMillis() % 1000000);
                break;
            }
        } while (deliveryRepository.findByTrackingNumber(candidate).isPresent());
        return candidate;
    }

    public DeliveryDTO toDTO(Delivery d) {
        DeliveryDTO dto = new DeliveryDTO();
        dto.setId(d.getId());
        dto.setDeliveryStatus(d.getDeliveryStatus().name());
        dto.setTrackingNumber(d.getTrackingNumber());
        dto.setDeliveryPartner(d.getDeliveryPartner());
        dto.setDeliveryPartnerPhone(d.getDeliveryPartnerPhone());
        dto.setEstimatedDeliveryDate(d.getEstimatedDeliveryDate());
        dto.setShippedAt(d.getShippedAt());
        dto.setOutForDeliveryAt(d.getOutForDeliveryAt());
        dto.setDeliveredAt(d.getDeliveredAt());
        dto.setDeliveryNotes(d.getDeliveryNotes());
        dto.setCreatedAt(d.getCreatedAt());
        dto.setUpdatedAt(d.getUpdatedAt());

        Order o = d.getOrder();
        if (o != null) {
            dto.setOrderId(o.getId());
            dto.setOrderNumber(o.getOrderNumber());
            dto.setOrderGrandTotal(o.getGrandTotal());
            dto.setOrderStatus(o.getOrderStatus().name());
            dto.setPaymentStatus(o.getPaymentStatus().name());
            dto.setPaymentMethod(o.getPaymentMethod());

            dto.setRecipientName(o.getRecipientName());
            dto.setRecipientPhone(o.getRecipientPhone());
            dto.setAddressLine1(o.getAddressLine1());
            dto.setAddressLine2(o.getAddressLine2());
            dto.setCity(o.getCity());
            dto.setState(o.getState());
            dto.setPincode(o.getPincode());

            User u = o.getUser();
            if (u != null) {
                dto.setCustomerName(u.getName());
                dto.setCustomerUsername(u.getUsername());
            }
        }

        return dto;
    }
}
