package com.smartagri.service;

import com.smartagri.dto.ConfirmPaymentRequest;
import com.smartagri.dto.PaymentDTO;
import com.smartagri.dto.UpdatePaymentStatusRequest;
import com.smartagri.entity.Order;
import com.smartagri.entity.OrderStatus;
import com.smartagri.entity.Payment;
import com.smartagri.entity.PaymentStatus;
import com.smartagri.repository.OrderRepository;
import com.smartagri.repository.PaymentRepository;
import com.smartagri.service.payment.MockPaymentGateway;
import com.smartagri.service.payment.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final MockPaymentGateway mockPaymentGateway;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          MockPaymentGateway mockPaymentGateway) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.mockPaymentGateway = mockPaymentGateway;
    }

    @Transactional
    public Payment createPaymentForOrder(Order order, String paymentMethod, String paymentReference) {
        // Idempotency: check if payment record already exists for this order
        Optional<Payment> existing = paymentRepository.findByOrderId(order.getId());
        if (existing.isPresent()) {
            Payment p = existing.get();
            if (p.getPaymentStatus().isSuccessful()) {
                log.info("Payment for order #{} is already successfully completed", order.getOrderNumber());
                return p;
            }
            if (paymentReference != null && !paymentReference.isBlank()) {
                p.setPaymentReference(paymentReference);
            }
            if (p.getUser() == null && order.getUser() != null) {
                p.setUser(order.getUser());
            }
            return paymentRepository.save(p);
        }

        String method = (paymentMethod != null && !paymentMethod.isBlank())
                ? paymentMethod.trim().toUpperCase(Locale.ROOT)
                : "COD";

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setUser(order.getUser());
        payment.setPaymentMethod(method);
        payment.setAmount(order.getGrandTotal());
        payment.setCurrency("INR");
        payment.setPaymentReference(paymentReference);

        String datePart = LocalDate.now().format(DATE_FMT);
        int randomSeq = ThreadLocalRandom.current().nextInt(100000, 999999);

        if ("ONLINE".equalsIgnoreCase(method)) {
            payment.setGatewayName(mockPaymentGateway.getGatewayName());
            payment.setPaymentStatus(PaymentStatus.INITIATED);
            payment.setTransactionId(String.format(Locale.ROOT, "TXN-INIT-%s-%06d", datePart, randomSeq));
            PaymentGateway.GatewayOrder gwOrder = mockPaymentGateway.createOrder(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getGrandTotal(),
                    order.getUser() != null ? order.getUser().getEmail() : null,
                    order.getRecipientPhone()
            );
            payment.setGatewayOrderId(gwOrder.gatewayOrderId());
        } else {
            payment.setGatewayName("SYSTEM_COD");
            payment.setPaymentStatus(PaymentStatus.PENDING);
            payment.setTransactionId(String.format(Locale.ROOT, "TXN-COD-%s-%06d", datePart, randomSeq));
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Created payment record #{} for order #{}", saved.getId(), order.getOrderNumber());
        return saved;
    }

    @Transactional(readOnly = true)
    public PaymentDTO getPaymentForOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this order.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("No payment found for order id: " + orderId));

        return toDTO(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDTO getPaymentById(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Payment not found with id: " + paymentId));

        if (payment.getOrder() != null && !payment.getOrder().getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this payment transaction.");
        }

        return toDTO(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> getCustomerPayments(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentDTO getPaymentAdmin(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("No payment found for order id: " + orderId));
        return toDTO(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDTO getPaymentAdminById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Payment not found with id: " + paymentId));
        return toDTO(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> getAllPaymentsAdmin(PaymentStatus status, String method, String search) {
        return paymentRepository.findAdminPaymentsFiltered(status, method, search).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(noRollbackFor = {IllegalArgumentException.class})
    public PaymentDTO processOnlinePaymentMock(Long userId, Long orderId, String paymentReference) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this order.");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled orders cannot be paid.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> createPaymentForOrder(order, "ONLINE", paymentReference));

        if (payment.getPaymentStatus().isSuccessful()) {
            throw new IllegalArgumentException("This order has already been successfully paid.");
        }

        PaymentGateway.GatewayResult result = mockPaymentGateway.processPayment(
                order.getId(), order.getOrderNumber(), order.getGrandTotal(), paymentReference);

        if (result.success()) {
            payment.setTransactionId(result.transactionId());
            payment.setGatewayPaymentId(result.gatewayPaymentId());
            payment.setGatewayResponse(result.gatewayResponse());
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setFailureReason(null);
            payment.setPaidAt(LocalDateTime.now());
            payment.setPaymentMethod("ONLINE");
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentMethod("ONLINE");
            orderRepository.save(order);
            log.info("Online test payment successful for order #{}, txnId: {}", order.getOrderNumber(), result.transactionId());
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(result.gatewayResponse());
            payment.setFailureReason(result.message());
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new IllegalArgumentException("Payment failed: " + result.message());
        }

        return toDTO(payment);
    }

    @Transactional
    public PaymentDTO initiateOnlinePayment(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this order.");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot initiate payment for a cancelled order.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> createPaymentForOrder(order, "ONLINE", null));

        if (payment.getPaymentStatus().isSuccessful()) {
            throw new IllegalArgumentException("This order has already been successfully paid.");
        }

        payment.setPaymentStatus(PaymentStatus.INITIATED);
        if (payment.getGatewayOrderId() == null) {
            PaymentGateway.GatewayOrder gwOrder = mockPaymentGateway.createOrder(
                    order.getId(), order.getOrderNumber(), order.getGrandTotal(),
                    order.getUser().getEmail(), order.getRecipientPhone());
            payment.setGatewayOrderId(gwOrder.gatewayOrderId());
        }
        paymentRepository.save(payment);

        return toDTO(payment);
    }

    @Transactional(noRollbackFor = {IllegalArgumentException.class})
    public PaymentDTO confirmPayment(Long userId, Long paymentId, ConfirmPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Payment record not found with id: " + paymentId));

        Order order = payment.getOrder();
        if (order == null) {
            throw new IllegalStateException("Payment is not attached to any order.");
        }

        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this order.");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot confirm payment for a cancelled order.");
        }

        // Idempotency: if already paid, return safely without duplicate processing
        if (payment.getPaymentStatus().isSuccessful()) {
            log.info("Payment #{} already confirmed and successful", payment.getId());
            return toDTO(payment);
        }

        // Authoritative Amount Verification: Never trust client amount
        if (request != null && request.getAmount() != null) {
            if (request.getAmount().compareTo(payment.getAmount()) != 0) {
                log.warn("Payment amount mismatch detected! Expected: {}, Submitted: {}",
                        payment.getAmount(), request.getAmount());
                throw new IllegalArgumentException("Payment amount mismatch: expected ₹" +
                        payment.getAmount() + " but received ₹" + request.getAmount());
            }
        }

        // Verify gateway result
        String gwOrderId = request != null && request.getGatewayOrderId() != null
                ? request.getGatewayOrderId() : payment.getGatewayOrderId();
        String gwPaymentId = request != null && request.getGatewayPaymentId() != null
                ? request.getGatewayPaymentId() : "pay_mock_" + System.currentTimeMillis();
        String signature = request != null ? request.getGatewaySignature() : "valid_signature";

        PaymentGateway.GatewayResult verifyResult = mockPaymentGateway.verifyPaymentSignature(
                gwOrderId, gwPaymentId, signature, payment.getAmount());

        if (verifyResult.success()) {
            if (!payment.getPaymentStatus().canTransitionTo(PaymentStatus.SUCCESS)) {
                throw new IllegalArgumentException("Invalid status transition from " +
                        payment.getPaymentStatus() + " to SUCCESS");
            }
            payment.setTransactionId(verifyResult.transactionId());
            payment.setGatewayPaymentId(gwPaymentId);
            payment.setGatewayResponse(verifyResult.gatewayResponse());
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setFailureReason(null);
            payment.setPaidAt(LocalDateTime.now());
            payment.setPaymentMethod("ONLINE");
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentMethod("ONLINE");
            orderRepository.save(order);
            log.info("Payment confirmed successfully for order #{}, txnId: {}", order.getOrderNumber(), verifyResult.transactionId());
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(verifyResult.gatewayResponse());
            payment.setFailureReason(verifyResult.message());
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new IllegalArgumentException("Payment verification failed: " + verifyResult.message());
        }

        return toDTO(payment);
    }

    @Transactional
    public PaymentDTO retryPayment(Long userId, Long orderId, String paymentReference) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this order.");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot retry payment for a cancelled order.");
        }

        if (order.getPaymentStatus().isSuccessful()) {
            throw new IllegalArgumentException("This order has already been successfully paid.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> createPaymentForOrder(order, "ONLINE", paymentReference));

        if (payment.getPaymentStatus().isSuccessful()) {
            throw new IllegalArgumentException("This order has already been successfully paid.");
        }

        // Generate fresh transaction ID and gateway order ID for this retry attempt
        String datePart = LocalDate.now().format(DATE_FMT);
        int randomSeq = ThreadLocalRandom.current().nextInt(100000, 999999);
        payment.setTransactionId(String.format(Locale.ROOT, "TXN-RETRY-%s-%06d", datePart, randomSeq));

        PaymentGateway.GatewayOrder gwOrder = mockPaymentGateway.createOrder(
                order.getId(), order.getOrderNumber(), order.getGrandTotal(),
                order.getUser().getEmail(), order.getRecipientPhone());
        payment.setGatewayOrderId(gwOrder.gatewayOrderId());
        payment.setGatewayPaymentId(null);
        payment.setGatewayResponse(null);
        payment.setFailureReason(null);
        payment.setPaymentReference(paymentReference);
        payment.setPaymentStatus(PaymentStatus.INITIATED);
        payment.setPaymentMethod("ONLINE");

        Payment saved = paymentRepository.save(payment);
        log.info("Initialized payment retry attempt for order #{}, new txnId: {}", order.getOrderNumber(), saved.getTransactionId());

        return toDTO(saved);
    }

    @Transactional
    public PaymentDTO updatePaymentStatusAdmin(Long orderId, UpdatePaymentStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> createPaymentForOrder(order, order.getPaymentMethod(), null));

        PaymentStatus newStatus;
        try {
            newStatus = PaymentStatus.valueOf(request.getPaymentStatus().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid payment status: " + request.getPaymentStatus());
        }

        PaymentStatus currentStatus = payment.getPaymentStatus();
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException("Invalid payment status transition from " + currentStatus + " to " + newStatus);
        }

        payment.setPaymentStatus(newStatus);
        if (request.getTransactionId() != null && !request.getTransactionId().isBlank()) {
            payment.setTransactionId(request.getTransactionId().trim());
        }
        if (request.getGatewayResponse() != null) {
            payment.setGatewayResponse(request.getGatewayResponse());
        }
        if (request.getFailureReason() != null) {
            payment.setFailureReason(request.getFailureReason().trim());
        }
        if (newStatus.isSuccessful() && payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }

        paymentRepository.save(payment);

        order.setPaymentStatus(newStatus);
        orderRepository.save(order);
        log.info("Admin updated payment status for order #{} from {} to {}", order.getOrderNumber(), currentStatus, newStatus);

        return toDTO(payment);
    }

    @Transactional
    public void refundPaymentIfApplicable(Order order) {
        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
            if (payment.getPaymentStatus().isSuccessful()) {
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                payment.setGatewayResponse("{\"refunded\":true,\"timestamp\":\"" + LocalDateTime.now() + "\"}");
                paymentRepository.save(payment);
                log.info("Payment refunded for cancelled order #{}", order.getOrderNumber());
            }
        });
    }

    public PaymentDTO toDTO(Payment p) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(p.getId());
        if (p.getOrder() != null) {
            dto.setOrderId(p.getOrder().getId());
            dto.setOrderNumber(p.getOrder().getOrderNumber());
            if (p.getOrder().getUser() != null) {
                dto.setUserId(p.getOrder().getUser().getId());
                dto.setCustomerName(p.getOrder().getUser().getName());
                dto.setCustomerUsername(p.getOrder().getUser().getUsername());
            }
        }
        if (p.getUser() != null) {
            dto.setUserId(p.getUser().getId());
            dto.setCustomerName(p.getUser().getName());
            dto.setCustomerUsername(p.getUser().getUsername());
        }
        dto.setTransactionId(p.getTransactionId());
        dto.setPaymentMethod(p.getPaymentMethod());
        dto.setPaymentStatus(p.getPaymentStatus().name());
        dto.setAmount(p.getAmount());
        dto.setCurrency(p.getCurrency());
        dto.setGatewayName(p.getGatewayName());
        dto.setGatewayOrderId(p.getGatewayOrderId());
        dto.setGatewayPaymentId(p.getGatewayPaymentId());
        dto.setFailureReason(p.getFailureReason());
        dto.setPaymentReference(p.getPaymentReference());
        dto.setPaidAt(p.getPaidAt());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());

        boolean eligibleForRetry = (p.getPaymentStatus() == PaymentStatus.FAILED ||
                p.getPaymentStatus() == PaymentStatus.PENDING ||
                p.getPaymentStatus() == PaymentStatus.INITIATED)
                && (p.getOrder() != null && p.getOrder().getOrderStatus() != OrderStatus.CANCELLED);
        dto.setCanRetry(eligibleForRetry);

        return dto;
    }
}
